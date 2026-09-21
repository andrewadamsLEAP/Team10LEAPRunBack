package com.example.services;

import com.example.repositories.MarketDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class MarketDataProcessor {

    private static final Logger logger =
            LoggerFactory.getLogger(
                    MarketDataProcessor.class);

    private final MarketDataRepository marketDataRepository;
    private final MarketSymbolMapper marketSymbolMapper;

    public MarketDataProcessor(
            MarketDataRepository marketDataRepository,
            MarketSymbolMapper marketSymbolMapper) {

        this.marketDataRepository =
                marketDataRepository;

        this.marketSymbolMapper =
                marketSymbolMapper;
    }

    public ProcessResult processQuotes(
            List<String> requestedSymbols,
            AlpacaClient.AlpacaQuotesResponse response) {

        if (response == null
                || response.quotes() == null) {

            throw new IllegalStateException(
                    "Alpaca returned an empty quotes response");
        }

        logger.info(
                "Alpaca returned {} quotes",
                response.quotes().size());

        int successfulCount = 0;
        int failedCount = 0;

        for (String ticker : requestedSymbols) {

            AlpacaClient.AlpacaQuote quote =
                    findQuote(
                            ticker,
                            response);

            if (quote == null) {

                failedCount++;

                logger.warn(
                        "Alpaca returned no quote for {}",
                        ticker);

                continue;
            }

            if (quote.askPrice() == null
                    || quote.askPrice().signum() <= 0) {

                failedCount++;

                logger.warn(
                        "Invalid ask price for {}",
                        ticker);

                continue;
            }

            OffsetDateTime quoteTimestamp =
                    quote.quoteTimestamp() != null
                            ? quote.quoteTimestamp()
                            : OffsetDateTime.now(
                                    ZoneOffset.UTC);

            marketDataRepository.saveQuote(
                    ticker,
                    quote.askPrice(),
                    quote.askSize(),
                    quote.askExchange(),
                    quote.bidPrice(),
                    quote.bidSize(),
                    quote.bidExchange(),
                    quote.tape(),
                    quoteTimestamp);

            successfulCount++;

            logger.info(
                    "Saved quote for {}: ask={}, bid={}, quoteTimestamp={}",
                    ticker,
                    quote.askPrice(),
                    quote.bidPrice(),
                    quoteTimestamp);
        }

        return new ProcessResult(
                successfulCount,
                failedCount);
    }

    private AlpacaClient.AlpacaQuote findQuote(
            String ticker,
            AlpacaClient.AlpacaQuotesResponse response) {

        AlpacaClient.AlpacaQuote quote =
                response.quotes().get(ticker);

        if (quote != null) {
            return quote;
        }

        /*
         * Crypto:
         *
         * BTC-USD <-> BTC/USD
         */
        if (marketSymbolMapper.isCryptoTicker(ticker)) {

            quote =
                    response.quotes().get(
                            marketSymbolMapper
                                    .toAlpacaCryptoSymbol(
                                            ticker));

            if (quote != null) {
                return quote;
            }
        }

        /*
         * Forex:
         *
         * EURUSD <-> EUR/USD
         */
        if (marketSymbolMapper.isForexTicker(ticker)) {

            quote =
                    response.quotes().get(
                            marketSymbolMapper
                                    .toAlpacaForexSymbol(
                                            ticker));

            if (quote != null) {
                return quote;
            }
        }

        return null;
    }

    public record ProcessResult(
            int successfulCount,
            int failedCount) {
    }
}