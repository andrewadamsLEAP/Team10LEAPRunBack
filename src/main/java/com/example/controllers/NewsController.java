package com.example.controllers;

import com.example.services.MarketNewsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/news")
public class NewsController {
    private final MarketNewsService marketNewsService;

    public NewsController(MarketNewsService marketNewsService) {
        this.marketNewsService = marketNewsService;
    }

    @GetMapping
    @PreAuthorize("hasRole('CLIENT')")
    public Map<String, Object> getNews(
            @RequestParam(required = false) String symbols,
            @RequestParam(defaultValue = "25") int limit,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "en") String language) {
        return marketNewsService.getNews(symbols, limit, page, language);
    }
}