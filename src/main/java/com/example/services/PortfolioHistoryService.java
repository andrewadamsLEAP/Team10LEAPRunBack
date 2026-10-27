package com.example.services;

import com.example.DTOs.clients.ClientProfileView;
import com.example.DTOs.holdings.HoldingResponse;
import com.example.DTOs.portfolio.PortfolioHistoryCoverage;
import com.example.DTOs.portfolio.PortfolioHistoryPoint;
import com.example.DTOs.portfolio.PortfolioHistoryPrice;
import com.example.DTOs.portfolio.PortfolioHistoryResponse;
import com.example.entities.Order;
import com.example.entities.Transaction;
import com.example.repositories.MarketDataRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PortfolioHistoryService {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final Duration MAX_HISTORY_RANGE = Duration.ofDays(366);
    private static final Duration MAX_PRICE_AGE = Duration.ofHours(72);

    private final ClientsService clientsService;
    private final HoldingsService holdingsService;
    private final OrdersService ordersService;
    private final TransactionsService transactionsService;
    private final MarketDataRepository marketDataRepository;

    public PortfolioHistoryService(
            ClientsService clientsService,
            HoldingsService holdingsService,
            OrdersService ordersService,
            TransactionsService transactionsService,
            MarketDataRepository marketDataRepository) {
        this.clientsService = clientsService;
        this.holdingsService = holdingsService;
        this.ordersService = ordersService;
        this.transactionsService = transactionsService;
        this.marketDataRepository = marketDataRepository;
    }

    public PortfolioHistoryResponse getHistory(
            Long clientId,
            OffsetDateTime requestedFrom,
            OffsetDateTime requestedTo) {
        if (clientId == null || clientId <= 0) {
            throw new IllegalArgumentException("Client ID must be greater than zero");
        }
        if (requestedFrom == null || requestedTo == null) {
            throw new IllegalArgumentException("Both from and to timestamps are required");
        }

        OffsetDateTime from = requestedFrom.withOffsetSameInstant(ZoneOffset.UTC);
        OffsetDateTime to = requestedTo.withOffsetSameInstant(ZoneOffset.UTC);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
        if (to.isAfter(now)) {
            throw new IllegalArgumentException("to must not be in the future");
        }
        if (Duration.between(from, to).compareTo(MAX_HISTORY_RANGE) > 0) {
            throw new IllegalArgumentException("History range must not exceed 366 days");
        }

        ClientProfileView profile = clientsService.getClientProfile(clientId);
        BigDecimal currentCash = requireValue(profile.cashAmount(), "Current cash balance is missing");
        List<HoldingResponse> currentHoldings = holdingsService.getAllClientHoldings(clientId);
        List<Order> fulfilledOrders = ordersService.getFulfilledOrders(clientId);
        List<Transaction> transactions = transactionsService.getTransactions(clientId);

        List<String> issues = new ArrayList<>(List.of(
                "ORDER_DATE_USED_AS_EXECUTION_TIME",
                "ORDER_DATE_TIME_ZONE_UNVERIFIED",
                "TRADE_FEES_NOT_RECORDED",
                "CORPORATE_ACTIONS_NOT_RECORDED",
                "EARLIEST_RECONSTRUCTIBLE_TIME_UNKNOWN"));

        Map<String, Integer> currentQuantities = new HashMap<>();
        Set<String> tickers = new LinkedHashSet<>();
        for (HoldingResponse holding : currentHoldings) {
            if (holding.ticker() == null || holding.quantity() == null) {
                issues.add("CURRENT_HOLDING_RECORD_INCOMPLETE");
                continue;
            }
            currentQuantities.put(holding.ticker(), holding.quantity());
            tickers.add(holding.ticker());
        }

        List<OrderEvent> orderEvents = new ArrayList<>();
        for (Order order : fulfilledOrders) {
            if (order.getOrderStatus() != Order.OrderStatus.FULFILLED) {
                continue;
            }
            if (order.getOrderDate() == null) {
                issues.add("FULFILLED_ORDER_WITHOUT_ORDER_DATE");
                continue;
            }
            if (order.getTicker() == null || order.getQuantity() <= 0
                    || order.getPrice() == null || order.getOrderType() == null) {
                issues.add("FULFILLED_ORDER_RECORD_INCOMPLETE");
                continue;
            }
            tickers.add(order.getTicker());
            orderEvents.add(new OrderEvent(
                    order.getOrderDate().withOffsetSameInstant(ZoneOffset.UTC),
                    order.getTicker(),
                    order.getOrderType(),
                    order.getQuantity(),
                    order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()))));
        }

        List<CashEvent> cashEvents = new ArrayList<>();
        for (Transaction transaction : transactions) {
            if (transaction.getTimestamp() == null) {
                issues.add("CASH_TRANSACTION_WITHOUT_TIMESTAMP");
                continue;
            }
            if (transaction.getAmount() == null || transaction.getAmount().signum() <= 0) {
                issues.add("CASH_TRANSACTION_AMOUNT_INVALID");
                continue;
            }
            BigDecimal signedAmount = switch (transaction.getType() == null
                    ? "" : transaction.getType().toUpperCase()) {
                case "DEPOSIT" -> transaction.getAmount();
                case "WITHDRAWAL", "PURCHASE" -> transaction.getAmount().negate();
                default -> {
                    issues.add("UNSUPPORTED_CASH_TRANSACTION_TYPE");
                    yield ZERO;
                }
            };
            cashEvents.add(new CashEvent(
                    transaction.getTimestamp().withOffsetSameInstant(ZoneOffset.UTC),
                    signedAmount));
        }

            orderEvents.sort(Comparator.comparing(event -> event.timestamp().toInstant()));
            cashEvents.sort(Comparator.comparing(event -> event.timestamp().toInstant()));

        List<OffsetDateTime> timestamps = sampleTimestamps(from, to);
        Map<PriceKey, PortfolioHistoryPrice> prices = new HashMap<>();
        if (!tickers.isEmpty()) {
            for (PortfolioHistoryPrice price : marketDataRepository.findPortfolioHistoryPrices(
                    timestamps, tickers.stream().sorted().toList())) {
                prices.put(new PriceKey(price.valuationTimestamp().toInstant(), price.ticker()), price);
            }
        }

        List<PortfolioHistoryPoint> points = new ArrayList<>(timestamps.size());
        Set<String> aggregateMissingPrices = new LinkedHashSet<>();
        for (OffsetDateTime timestamp : timestamps) {
            Map<String, Integer> quantities = new HashMap<>(currentQuantities);
            BigDecimal cash = currentCash;

            for (OrderEvent event : orderEvents) {
                if (event.timestamp().isAfter(timestamp)) {
                    int signedQuantity = event.orderType() == Order.OrderType.BUY
                            ? event.quantity() : -event.quantity();
                    quantities.merge(event.ticker(), -signedQuantity, Integer::sum);
                    BigDecimal signedCash = event.orderType() == Order.OrderType.BUY
                            ? event.amount().negate() : event.amount();
                    cash = cash.subtract(signedCash);
                }
            }
            for (CashEvent event : cashEvents) {
                if (event.timestamp().isAfter(timestamp)) {
                    cash = cash.subtract(event.signedAmount());
                }
            }

            BigDecimal invested = ZERO;
            List<String> missingPrices = new ArrayList<>();
            boolean validQuantities = true;
            for (Map.Entry<String, Integer> holding : quantities.entrySet()) {
                if (holding.getValue() < 0) {
                    validQuantities = false;
                    issues.add("NEGATIVE_RECONSTRUCTED_HOLDING:" + holding.getKey());
                    continue;
                }
                if (holding.getValue() == 0) {
                    continue;
                }
                PortfolioHistoryPrice price = prices.get(new PriceKey(timestamp.toInstant(), holding.getKey()));
                if (price == null || price.bidPrice() == null || price.priceTimestamp() == null
                        || price.priceTimestamp().isAfter(timestamp)
                        || Duration.between(price.priceTimestamp(), timestamp).compareTo(MAX_PRICE_AGE) > 0) {
                    missingPrices.add(holding.getKey());
                    aggregateMissingPrices.add(holding.getKey());
                    continue;
                }
                invested = invested.add(price.bidPrice().multiply(BigDecimal.valueOf(holding.getValue())));
            }

            boolean pointComplete = missingPrices.isEmpty() && validQuantities;
            BigDecimal roundedCash = cash.setScale(2, RoundingMode.HALF_UP);
            if (missingPrices.isEmpty() && validQuantities) {
                BigDecimal roundedInvested = invested.setScale(2, RoundingMode.HALF_UP);
                points.add(new PortfolioHistoryPoint(
                        timestamp,
                        roundedCash,
                        roundedInvested,
                        roundedCash.add(roundedInvested),
                        pointComplete,
                        List.of()));
            } else {
                points.add(new PortfolioHistoryPoint(
                        timestamp,
                        roundedCash,
                        null,
                        null,
                        false,
                        missingPrices.stream().sorted().toList()));
            }
        }

        if (!aggregateMissingPrices.isEmpty()) {
            issues.add("MISSING_OR_STALE_BID_PRICES");
        }
        return new PortfolioHistoryResponse(
                "USD",
                new PortfolioHistoryCoverage(false, null, issues.stream().distinct().toList()),
                points);
    }

    private List<OffsetDateTime> sampleTimestamps(OffsetDateTime from, OffsetDateTime to) {
        Duration duration = Duration.between(from, to);
        Duration interval = duration.compareTo(Duration.ofDays(1)) <= 0
            ? Duration.ofHours(1)
                : duration.compareTo(Duration.ofDays(7)) <= 0
                ? Duration.ofHours(6)
                        : duration.compareTo(Duration.ofDays(31)) <= 0
                                ? Duration.ofDays(1)
                                : Duration.ofDays(7);

        List<OffsetDateTime> timestamps = new ArrayList<>();
        OffsetDateTime current = from;
        timestamps.add(current);
        while (current.plus(interval).isBefore(to)) {
            current = current.plus(interval);
            timestamps.add(current);
        }
        if (!timestamps.get(timestamps.size() - 1).equals(to)) {
            timestamps.add(to);
        }
        return timestamps;
    }

    private BigDecimal requireValue(BigDecimal value, String message) {
        if (value == null) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    private record OrderEvent(
            OffsetDateTime timestamp,
            String ticker,
            Order.OrderType orderType,
            int quantity,
            BigDecimal amount) {
    }

    private record CashEvent(OffsetDateTime timestamp, BigDecimal signedAmount) {
    }

    private record PriceKey(java.time.Instant timestamp, String ticker) {
    }
}