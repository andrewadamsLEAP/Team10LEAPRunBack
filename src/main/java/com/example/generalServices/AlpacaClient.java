package com.example.generalServices;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AlpacaClient {

private final RestClient stocksClient;
private final RestClient cryptoClient;
private final RestClient forexClient;

private final String apiKey;
private final String apiSecret;

private final int maxRetries;
private final long retryBackoffMs;

public AlpacaClient(

        @Value("${alpaca.stocks-base-url:https://data.alpaca.markets/v2/stocks}")
        String stocksBaseUrl,

        @Value("${alpaca.crypto-base-url:https://data.alpaca.markets/v1beta3/crypto/us}")
        String cryptoBaseUrl,

        @Value("${alpaca.forex-base-url:https://data.alpaca.markets/v1beta1/forex/latest/rates}")
        String forexBaseUrl,

        @Value("${alpaca.api-key:}")
        String apiKey,

        @Value("${alpaca.api-secret:}")
        String apiSecret,

        @Value("${alpaca.request-timeout-ms:5000}")
        long requestTimeoutMs,

        @Value("${alpaca.max-retries:2}")
        int maxRetries,

        @Value("${alpaca.retry-backoff-ms:2500}")
        long retryBackoffMs) {

    this.apiKey = apiKey;
    this.apiSecret = apiSecret;

    this.maxRetries =
            Math.max(0, maxRetries);

    this.retryBackoffMs =
            Math.max(0, retryBackoffMs);

    // =========================================================
    // HTTP Request Factory
    // =========================================================

    JdkClientHttpRequestFactory requestFactory =
            new JdkClientHttpRequestFactory(
                    HttpClient.newBuilder()
                            .connectTimeout(
                                    Duration.ofMillis(
                                            requestTimeoutMs))
                            .build());

    requestFactory.setReadTimeout(
            Duration.ofMillis(
                    requestTimeoutMs));

    // =========================================================
    // Stock Client
    // =========================================================

    this.stocksClient =
            RestClient.builder()
                    .baseUrl(stocksBaseUrl)
                    .requestFactory(requestFactory)
                    .build();

    // =========================================================
    // Crypto Client
    // =========================================================

    this.cryptoClient =
            RestClient.builder()
                    .baseUrl(cryptoBaseUrl)
                    .requestFactory(requestFactory)
                    .build();

    // =========================================================
    // Forex Client
    //
    // Base URL:
    // https://data.alpaca.markets/v1beta1/forex/latest/rates
    // =========================================================

    this.forexClient =
            RestClient.builder()
                    .baseUrl(forexBaseUrl)
                    .requestFactory(requestFactory)
                    .build();
}

// =========================================================
// Credential Helpers
// =========================================================

public boolean isApiKeyConfigured() {

    return apiKey != null
            && !apiKey.isBlank();
}

public boolean isApiSecretConfigured() {

    return apiSecret != null
            && !apiSecret.isBlank();
}

public void validateCredentials() {

    if (!isApiKeyConfigured()) {

        throw new IllegalStateException(
                "ALPACA_API_KEY is not configured");
    }

    if (!isApiSecretConfigured()) {

        throw new IllegalStateException(
                "ALPACA_API_SECRET is not configured");
    }
}

// =========================================================
// Stock API
// =========================================================

public AlpacaQuotesResponse getStockQuotes(
        List<String> symbols) {

    String symbolParameter =
            String.join(",", symbols);

    return executeQuoteRequest(
            stocksClient,
            "/quotes/latest",
            symbolParameter);
}

// =========================================================
// Crypto API
// =========================================================

public AlpacaQuotesResponse getCryptoQuotes(
        String symbols) {

    return executeQuoteRequest(
            cryptoClient,
            "/latest/quotes",
            symbols);
}

// =========================================================
// Forex API
//
// Alpaca endpoint:
//
// GET /v1beta1/forex/latest/rates
//
// Parameter:
//
// currency_pairs=USDJPY,EURUSD
//
// IMPORTANT:
// Forex does NOT use the "symbols" parameter.
// =========================================================

public AlpacaForexResponse getForexRates(
        String currencyPairs) {

    return executeForexRequest(
            currencyPairs);
}

// =========================================================
// Stock / Crypto Request
// =========================================================

private AlpacaQuotesResponse executeQuoteRequest(
        RestClient client,
        String path,
        String symbols) {

    for (int attempt = 0;
         attempt <= maxRetries;
         attempt++) {

        try {

            return client.get()
                    .uri(uriBuilder ->
                            uriBuilder
                                    .path(path)
                                    .queryParam(
                                            "symbols",
                                            symbols)
                                    .build())
                    .header(
                            "APCA-API-KEY-ID",
                            apiKey)
                    .header(
                            "APCA-API-SECRET-KEY",
                            apiSecret)
                    .retrieve()
                    .body(
                            AlpacaQuotesResponse.class);

        } catch (RestClientException exception) {

            if (attempt == maxRetries) {

                throw exception;
            }

            System.out.println(
                    "Alpaca request failed. " +
                    "Retrying " +
                    (attempt + 1) +
                    "/" +
                    maxRetries +
                    ". Error: " +
                    exception.getMessage());

            sleepBeforeRetry();
        }
    }

    throw new IllegalStateException(
            "Quote request did not produce a result");
}

// =========================================================
// Forex Request
// =========================================================

private AlpacaForexResponse executeForexRequest(
        String currencyPairs) {

    for (int attempt = 0;
         attempt <= maxRetries;
         attempt++) {

        try {

            return forexClient.get()
                    .uri(uriBuilder ->
                            uriBuilder
                                    .queryParam(
                                            "currency_pairs",
                                            currencyPairs)
                                    .build())
                    .header(
                            "APCA-API-KEY-ID",
                            apiKey)
                    .header(
                            "APCA-API-SECRET-KEY",
                            apiSecret)
                    .retrieve()
                    .body(
                            AlpacaForexResponse.class);

        } catch (RestClientException exception) {

            if (attempt == maxRetries) {

                throw exception;
            }

            System.out.println(
                    "Alpaca Forex request failed. " +
                    "Retrying " +
                    (attempt + 1) +
                    "/" +
                    maxRetries +
                    ". Error: " +
                    exception.getMessage());

            sleepBeforeRetry();
        }
    }

    throw new IllegalStateException(
            "Forex request did not produce a result");
}

// =========================================================
// Retry Helper
// =========================================================

private void sleepBeforeRetry() {

    try {

        Thread.sleep(
                retryBackoffMs);

    } catch (InterruptedException interruptedException) {

        Thread.currentThread().interrupt();

        throw new IllegalStateException(
                "Alpaca request retry was interrupted",
                interruptedException);
    }
}

// =========================================================
// Stock / Crypto Response Models
// =========================================================

public record AlpacaQuotesResponse(

        @JsonProperty("quotes")
        Map<String, AlpacaQuote> quotes) {
}

public record AlpacaQuote(

        @JsonProperty("ap")
        BigDecimal askPrice,

        @JsonProperty("as")
        BigDecimal askSize,

        @JsonProperty("ax")
        String askExchange,

        @JsonProperty("bp")
        BigDecimal bidPrice,

        @JsonProperty("bs")
        BigDecimal bidSize,

        @JsonProperty("bx")
        String bidExchange,

        @JsonProperty("c")
        List<String> conditions,

        @JsonProperty("t")
        OffsetDateTime quoteTimestamp,

        @JsonProperty("z")
        String tape) {
}

// =========================================================
// Forex Response Models
// =========================================================

public record AlpacaForexResponse(

        @JsonProperty("rates")
        Map<String, AlpacaForexRate> rates) {
}

public record AlpacaForexRate(

        @JsonProperty("bp")
        BigDecimal bidPrice,

        @JsonProperty("bx")
        String bidExchange,

        @JsonProperty("ap")
        BigDecimal askPrice,

        @JsonProperty("ax")
        String askExchange,

        @JsonProperty("t")
        OffsetDateTime quoteTimestamp) {
}

}
