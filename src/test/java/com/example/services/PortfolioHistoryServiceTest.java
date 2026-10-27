package com.example.services;

import com.example.DTOs.clients.ClientProfileView;
import com.example.DTOs.holdings.HoldingResponse;
import com.example.DTOs.portfolio.PortfolioHistoryPrice;
import com.example.DTOs.portfolio.PortfolioHistoryResponse;
import com.example.entities.Order;
import com.example.repositories.MarketDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyList;

@ExtendWith(MockitoExtension.class)
class PortfolioHistoryServiceTest {
    private static final Long CLIENT_ID = 12L;
    private static final OffsetDateTime FROM = OffsetDateTime.parse("2026-10-26T09:00:00Z");
    private static final OffsetDateTime TO = OffsetDateTime.parse("2026-10-26T10:00:00Z");

    @Mock
    private ClientsService clientsService;
    @Mock
    private HoldingsService holdingsService;
    @Mock
    private OrdersService ordersService;
    @Mock
    private TransactionsService transactionsService;
    @Mock
    private MarketDataRepository marketDataRepository;

    private PortfolioHistoryService service;

    @BeforeEach
    void setUp() {
        service = new PortfolioHistoryService(
                clientsService,
                holdingsService,
                ordersService,
                transactionsService,
                marketDataRepository);
    }

    @Test
    void buildsChronologicalCashPointsAndReportsUnverifiedCoverage() {
        when(clientsService.getClientProfile(CLIENT_ID)).thenReturn(
                new ClientProfileView(CLIENT_ID, "client@example.com", "client", "A", "B",
                        new BigDecimal("1200.00")));
        when(holdingsService.getAllClientHoldings(CLIENT_ID)).thenReturn(List.of());
        when(ordersService.getFulfilledOrders(CLIENT_ID)).thenReturn(List.of());
        when(transactionsService.getTransactions(CLIENT_ID)).thenReturn(List.of());

        PortfolioHistoryResponse response = service.getHistory(CLIENT_ID, FROM, TO);

        assertEquals("USD", response.currency());
        assertEquals(2, response.points().size());
        assertEquals(FROM, response.points().get(0).timestamp());
        assertEquals(TO, response.points().get(response.points().size() - 1).timestamp());
        assertEquals(new BigDecimal("1200.00"), response.points().get(0).cash());
        assertEquals(new BigDecimal("0.00"), response.points().get(0).invested());
        assertEquals(new BigDecimal("1200.00"), response.points().get(0).total());
        assertFalse(response.coverage().complete());
        assertEquals(null, response.coverage().earliestReconstructibleAt());
    }

    @Test
    void rejectsRangesThatRunIntoTheFutureBeforeLoadingRecords() {
        OffsetDateTime future = OffsetDateTime.now().plusDays(1);

        assertThrows(IllegalArgumentException.class,
                () -> service.getHistory(CLIENT_ID, FROM, future));

        verify(clientsService, never()).getClientProfile(CLIENT_ID);
    }

    @Test
    void usesHourlyDayAndSixHourWeekSamplesWithoutChangingMonthlySampling() {
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC)
                .minusMinutes(1)
                .withSecond(0)
                .withNano(0);
        when(clientsService.getClientProfile(CLIENT_ID)).thenReturn(
                new ClientProfileView(CLIENT_ID, "client@example.com", "client", "A", "B",
                        new BigDecimal("1200.00")));
        when(holdingsService.getAllClientHoldings(CLIENT_ID)).thenReturn(List.of());
        when(ordersService.getFulfilledOrders(CLIENT_ID)).thenReturn(List.of());
        when(transactionsService.getTransactions(CLIENT_ID)).thenReturn(List.of());

        PortfolioHistoryResponse day = service.getHistory(CLIENT_ID, to.minusDays(1), to);
        PortfolioHistoryResponse week = service.getHistory(CLIENT_ID, to.minusDays(7), to);
        PortfolioHistoryResponse month = service.getHistory(CLIENT_ID, to.minusDays(30), to);

        assertEquals(25, day.points().size());
        assertEquals(29, week.points().size());
        assertEquals(31, month.points().size());
    }

    @Test
    void usesOrderDateToReverseCompletedTradesAtEachSample() {
    OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).minusDays(2)
        .withSecond(0).withNano(0);
    OffsetDateTime tradeTime = from.plusMinutes(30);
    OffsetDateTime to = from.plusDays(1);
    Order completedBuy = new Order(
        1L,
        CLIENT_ID,
        "XYZ",
        Order.OrderType.BUY,
        Order.OrderStatus.FULFILLED,
        2,
        new BigDecimal("10.00"),
        tradeTime);

    when(clientsService.getClientProfile(CLIENT_ID)).thenReturn(
        new ClientProfileView(CLIENT_ID, "client@example.com", "client", "A", "B",
            new BigDecimal("950.00")));
    when(holdingsService.getAllClientHoldings(CLIENT_ID)).thenReturn(
        List.of(new HoldingResponse(CLIENT_ID, "XYZ", 5)));
    when(ordersService.getFulfilledOrders(CLIENT_ID)).thenReturn(List.of(completedBuy));
    when(transactionsService.getTransactions(CLIENT_ID)).thenReturn(List.of());
    when(marketDataRepository.findPortfolioHistoryPrices(anyList(), anyList()))
        .thenAnswer(invocation -> {
            List<OffsetDateTime> timestamps = invocation.getArgument(0);
            return timestamps.stream()
                .map(timestamp -> new PortfolioHistoryPrice(
                    timestamp,
                    "XYZ",
                    new BigDecimal("10.00"),
                    timestamp))
                .toList();
        });

    PortfolioHistoryResponse response = service.getHistory(CLIENT_ID, from, to);

    assertEquals(new BigDecimal("970.00"), response.points().get(0).cash());
    assertEquals(new BigDecimal("30.00"), response.points().get(0).invested());
    assertEquals(new BigDecimal("950.00"), response.points().get(1).cash());
    assertEquals(new BigDecimal("50.00"), response.points().get(1).invested());
    assertTrue(response.points().get(0).complete());
    assertTrue(response.points().get(1).complete());
    }

    @Test
    void reportsMissingPricesInsteadOfReturningZeroInvestedValue() {
    OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1);
    OffsetDateTime to = from.plusMinutes(15);

    when(clientsService.getClientProfile(CLIENT_ID)).thenReturn(
        new ClientProfileView(CLIENT_ID, "client@example.com", "client", "A", "B",
            new BigDecimal("500.00")));
    when(holdingsService.getAllClientHoldings(CLIENT_ID)).thenReturn(
        List.of(new HoldingResponse(CLIENT_ID, "XYZ", 4)));
    when(ordersService.getFulfilledOrders(CLIENT_ID)).thenReturn(List.of());
    when(transactionsService.getTransactions(CLIENT_ID)).thenReturn(List.of());
    when(marketDataRepository.findPortfolioHistoryPrices(anyList(), anyList()))
        .thenReturn(List.of());

    PortfolioHistoryResponse response = service.getHistory(CLIENT_ID, from, to);

    assertEquals(null, response.points().get(0).invested());
    assertEquals(null, response.points().get(0).total());
    assertEquals(List.of("XYZ"), response.points().get(0).missingPrices());
    }
}