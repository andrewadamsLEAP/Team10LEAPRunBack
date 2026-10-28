package com.example.generalServices;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
public class FrankfurterClient {
    private static final Logger logger = LoggerFactory.getLogger(FrankfurterClient.class);

    private final RestClient client;
    private final int maxRetries;
    private final long retryBackoffMs;

    public FrankfurterClient(
            @Value("${frankfurter.base-url:https://api.frankfurter.dev/v2/rates}") String baseUrl,
            @Value("${alpaca.request-timeout-ms:5000}") long requestTimeoutMs,
            @Value("${alpaca.max-retries:2}") int maxRetries,
            @Value("${alpaca.retry-backoff-ms:2500}") long retryBackoffMs) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(requestTimeoutMs))
                        .build());
        requestFactory.setReadTimeout(Duration.ofMillis(requestTimeoutMs));

        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        this.maxRetries = Math.max(0, maxRetries);
        this.retryBackoffMs = Math.max(0, retryBackoffMs);
    }

    public List<FrankfurterRate> getRates(String base, List<String> quotes) {
        return getRates(base, quotes, null, null);
    }

    public List<FrankfurterRate> getRates(
            String base,
            List<String> quotes,
            LocalDate from,
            LocalDate to) {
        if (quotes == null || quotes.isEmpty()) {
            return List.of();
        }
        if ((from == null) != (to == null)) {
            throw new IllegalArgumentException("Both from and to dates must be provided together");
        }
        if (from != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from date must not be after to date");
        }

        String quoteParameter = String.join(",", quotes);
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                FrankfurterRate[] response = client.get()
                        .uri(uriBuilder -> uriBuilder
                                .queryParam("base", base)
                                .queryParam("quotes", quoteParameter)
                                .queryParamIfPresent("from", java.util.Optional.ofNullable(from))
                                .queryParamIfPresent("to", java.util.Optional.ofNullable(to))
                                .build())
                        .retrieve()
                        .body(FrankfurterRate[].class);
                return response == null ? List.of() : Arrays.asList(response);
            } catch (RestClientException exception) {
                if (attempt == maxRetries) {
                    logger.error("Frankfurter request failed after {} retries", maxRetries, exception);
                    throw exception;
                }
                logger.warn("Frankfurter request failed. Retrying {}/{}: {}",
                        attempt + 1, maxRetries, exception.getMessage());
                sleepBeforeRetry();
            }
        }

        throw new IllegalStateException("Frankfurter request did not produce a result");
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(retryBackoffMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Frankfurter request retry was interrupted", exception);
        }
    }

    public record FrankfurterRate(
            LocalDate date,
            String base,
            String quote,
            BigDecimal rate) {
    }
}
