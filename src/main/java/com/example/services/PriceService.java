package com.example.services;

import com.example.entities.Prices;
import com.example.repositories.PriceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Service
public class PriceService {
    private final PriceRepository priceRepository;

    public PriceService(PriceRepository priceRepository) {
        this.priceRepository = priceRepository;
    }

    @Value("${finnhub.api.key}")
    private String finnhubKey;
    String baseurl = "https://finnhub.io/api/v1/quote?symbol=";
    public static final List<String> TICKERS = List.of("AAPL");

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void pullData() throws Exception {
        if (finnhubKey == null || finnhubKey.isEmpty()) {
            throw new IllegalStateException(
                    "FINNHUB API KEY not being grabbed properly or not set"
            );
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(1000))
                .build();

        for (String t : TICKERS) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseurl + t + "&token=" + finnhubKey))
                    .GET()
                    .build();

            try {
                HttpResponse<String> response = client.send(request,
                        HttpResponse.BodyHandlers.ofString());

                System.out.println("Response Status Code: " + response.statusCode());
                System.out.println("Response Body: " + response.body());

                ObjectMapper objectMapper = new ObjectMapper();

                Prices price = objectMapper.readValue(response.body(),Prices.class);

                price.setTicker(t);

                priceRepository.save(price);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Failed to receive stock data for ticker: " + t, e);
            }
        }
    }
}
