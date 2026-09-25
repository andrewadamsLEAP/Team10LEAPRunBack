package com.example.marketData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.generalServices.AlpacaClient;
import com.example.generalServices.AlpacaClient.AlpacaForexRate;
import com.example.generalServices.AlpacaClient.AlpacaForexResponse;
import com.example.generalServices.AlpacaClient.AlpacaQuote;
import com.example.generalServices.AlpacaClient.AlpacaQuotesResponse;

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

// =========================================================
// STOCK / CRYPTO QUOTE PROCESSING
// =========================================================

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

// =========================================================
// FOREX RATE PROCESSING
// =========================================================

public ProcessResult processForexRates(
        List<String> requestedSymbols,
        AlpacaClient.AlpacaForexResponse response) {

    if (response == null
            || response.rates() == null) {

        throw new IllegalStateException(
                "Alpaca returned an empty forex rates response");
    }

    logger.info(
            "Alpaca returned {} forex rates",
            response.rates().size());

    int successfulCount = 0;
    int failedCount = 0;

    for (String ticker : requestedSymbols) {

        AlpacaClient.AlpacaForexRate rate =
                findForexRate(
                        ticker,
                        response);

        if (rate == null) {

            failedCount++;

            logger.warn(
                    "Alpaca returned no forex rate for {}",
                    ticker);

            continue;
        }

        if (rate.askPrice() == null
                || rate.askPrice().signum() <= 0) {

            failedCount++;

            logger.warn(
                    "Invalid forex ask price for {}",
                    ticker);

            continue;
        }

        OffsetDateTime quoteTimestamp =
                rate.quoteTimestamp() != null
                        ? rate.quoteTimestamp()
                        : OffsetDateTime.now(
                                ZoneOffset.UTC);

        marketDataRepository.saveQuote(
                ticker,
                rate.askPrice(),
                null,
                rate.askExchange(),
                rate.bidPrice(),
                null,
                rate.bidExchange(),
                null,
                quoteTimestamp);

        successfulCount++;

        logger.info(
                "Saved forex rate for {}: ask={}, bid={}, quoteTimestamp={}",
                ticker,
                rate.askPrice(),
                rate.bidPrice(),
                quoteTimestamp);
    }

    return new ProcessResult(
            successfulCount,
            failedCount);
}

// =========================================================
// STOCK / CRYPTO QUOTE LOOKUP
// =========================================================

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
     * This is retained for compatibility with
     * any quote-style Forex response.
     *
     * The current Alpaca Forex endpoint uses
     * AlpacaForexResponse and is handled by
     * findForexRate() below.
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

// =========================================================
// FOREX RATE LOOKUP
// =========================================================

private AlpacaClient.AlpacaForexRate findForexRate(
        String ticker,
        AlpacaClient.AlpacaForexResponse response) {

    /*
     * First try the ticker exactly as it appears
     * in the database.
     *
     * Example:
     *
     * EURUSD
     */
    AlpacaClient.AlpacaForexRate rate =
            response.rates().get(ticker);

    if (rate != null) {

        return rate;
    }

    /*
     * Convert the database symbol into the
     * Alpaca Forex symbol.
     *
     * Example:
     *
     * EURUSD -> EUR/USD
     */
    String alpacaSymbol =
            marketSymbolMapper
                    .toAlpacaForexSymbol(
                            ticker);

    rate =
            response.rates().get(
                    alpacaSymbol);

    if (rate != null) {

        return rate;
    }

    /*
     * Some API responses may use the symbol
     * without the slash.
     *
     * Example:
     *
     * EUR/USD -> EURUSD
     */
    String normalizedTicker =
            ticker.replace(
                    "/",
                    "").toUpperCase();

    for (var entry :
            response.rates().entrySet()) {

        String responseSymbol =
                entry.getKey();

        if (responseSymbol == null) {

            continue;
        }

        String normalizedResponseSymbol =
                responseSymbol
                        .replace(
                                "/",
                                "")
                        .toUpperCase();

        if (normalizedTicker.equals(
                normalizedResponseSymbol)) {

            return entry.getValue();
        }
    }

    return null;
}

// =========================================================
// PROCESS RESULT
// =========================================================

public record ProcessResult(
        int successfulCount,
        int failedCount) {
}

}
