package com.example.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MarketNewsServiceTest {
    @Test
    void requestsMarketauxNewsWithServerSideTokenAndFilters() {
        RestClient.Builder builder = RestClient.builder()
                .messageConverters(converters -> converters.add(new MappingJackson2HttpMessageConverter()));
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MarketNewsService service = new MarketNewsService(
                builder,
                new ObjectMapper(),
                "https://api.marketaux.com/v1/news/all",
                "test-token");

        server.expect(requestTo("https://api.marketaux.com/v1/news/all?api_token=test-token&language=en"
                        + "&limit=2&page=3&group_similar=true&symbols=AAPL"))
                .andExpect(queryParam("api_token", "test-token"))
                .andExpect(queryParam("symbols", "AAPL"))
                .andExpect(queryParam("limit", "2"))
                .andExpect(queryParam("page", "3"))
                .andRespond(withSuccess("""
                        {"meta":{"returned":0,"limit":2,"page":3},"data":[]}
                        """, MediaType.APPLICATION_JSON));

        var response = service.getNews("AAPL", 2, 3, "en");

        assertTrue(response.get("meta") instanceof java.util.Map<?, ?>);
        assertTrue(response.get("data") instanceof java.util.List<?>);
        assertEquals(0, ((java.util.Map<?, ?>) response.get("meta")).get("returned"));
        assertEquals(0, ((java.util.List<?>) response.get("data")).size());
        assertTrue(new ObjectMapper().valueToTree(response).path("data").isArray());
        server.verify();
    }

    @Test
    void returnsServiceUnavailableWhenMarketNewsTokenIsMissing() {
        MarketNewsService service = new MarketNewsService(
                RestClient.builder(),
                new ObjectMapper(),
                "https://api.marketaux.com/v1/news/all",
                " ");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getNews(null, 25, 1, "en"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
    }
}