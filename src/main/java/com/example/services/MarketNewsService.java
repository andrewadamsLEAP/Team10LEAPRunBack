package com.example.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class MarketNewsService {
    private static final Logger logger = LoggerFactory.getLogger(MarketNewsService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiToken;

    public MarketNewsService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${market.news.base-url:https://api.marketaux.com/v1/news/all}") String baseUrl,
            @Value("${market.news.api-key:${MARKET_NEWS_API:}}") String apiToken) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.apiToken = apiToken;
    }

    public Map<String, Object> getNews(String symbols, int limit, int page, String language) {
        if (apiToken == null || apiToken.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Market news provider is not configured");
        }
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("limit must be between 1 and 100");
        }
        if (page < 1) {
            throw new IllegalArgumentException("page must be greater than zero");
        }
        if (symbols != null && symbols.length() > 250) {
            throw new IllegalArgumentException("symbols must not exceed 250 characters");
        }
        if (language == null || !language.matches("[a-z]{2}(,[a-z]{2})*")) {
            throw new IllegalArgumentException("language must be a comma-separated list of language codes");
        }

        try {
            String body = restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .queryParam("api_token", apiToken)
                                .queryParam("language", language)
                                .queryParam("limit", limit)
                                .queryParam("page", page)
                                .queryParam("group_similar", true);
                        if (symbols != null && !symbols.isBlank()) {
                            builder.queryParam("symbols", symbols);
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .body(String.class);
            if (body == null || body.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Market news provider returned an empty response");
            }
            try {
                return objectMapper.readValue(body, new TypeReference<>() { });
            } catch (Exception exception) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Market news provider returned invalid JSON");
            }
        } catch (RestClientException exception) {
            logger.warn("Marketaux news request failed: {}", exception.getClass().getSimpleName());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Market news provider request failed");
        }
    }
}