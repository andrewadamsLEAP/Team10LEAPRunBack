package com.example.controllers;

import com.example.DTOs.portfolio.PortfolioHistoryResponse;
import com.example.authUtils.ClientIdContextHolder;
import com.example.services.PortfolioHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/portfolio")
public class PortfolioHistoryController {
    private static final Logger logger = LoggerFactory.getLogger(PortfolioHistoryController.class);

    private final PortfolioHistoryService portfolioHistoryService;

    public PortfolioHistoryController(PortfolioHistoryService portfolioHistoryService) {
        this.portfolioHistoryService = portfolioHistoryService;
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('CLIENT')")
    public PortfolioHistoryResponse getHistory(
            @RequestParam OffsetDateTime from,
            @RequestParam OffsetDateTime to) {
        long startedAtNanos = System.nanoTime();
        try {
            PortfolioHistoryResponse response = portfolioHistoryService.getHistory(
                    ClientIdContextHolder.requireClientId(),
                    from,
                    to);
            logger.info(
                    "Portfolio history request completed from={} to={} points={} elapsedMs={}",
                    from,
                    to,
                    response.points().size(),
                    (System.nanoTime() - startedAtNanos) / 1_000_000);
            return response;
        } catch (RuntimeException exception) {
            logger.error(
                    "Portfolio history request failed from={} to={} elapsedMs={}",
                    from,
                    to,
                    (System.nanoTime() - startedAtNanos) / 1_000_000,
                    exception);
            throw exception;
        }
    }
}