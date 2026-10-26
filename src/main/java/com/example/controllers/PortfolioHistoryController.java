package com.example.controllers;

import com.example.DTOs.portfolio.PortfolioHistoryResponse;
import com.example.authUtils.ClientIdContextHolder;
import com.example.services.PortfolioHistoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/portfolio")
public class PortfolioHistoryController {
    private final PortfolioHistoryService portfolioHistoryService;

    public PortfolioHistoryController(PortfolioHistoryService portfolioHistoryService) {
        this.portfolioHistoryService = portfolioHistoryService;
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('CLIENT')")
    public PortfolioHistoryResponse getHistory(
            @RequestParam OffsetDateTime from,
            @RequestParam OffsetDateTime to) {
        return portfolioHistoryService.getHistory(
                ClientIdContextHolder.requireClientId(),
                from,
                to);
    }
}