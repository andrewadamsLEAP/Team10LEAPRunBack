package com.example.controllers;

import com.example.entities.Transaction;
import com.example.services.TransactionsService;
import com.example.authUtils.ClientIdContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/trading")
public class TransactionsController {

    private final TransactionsService transactionsService;

    public TransactionsController(TransactionsService transactionsService) {
        this.transactionsService = transactionsService;
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasRole('CLIENT')")
    public List<Transaction> getTransactions(
            @RequestParam Long clientId) {
        ClientIdContextHolder.requireClient(clientId);

        return transactionsService.getTransactions(clientId);
    }

    @PostMapping("/transactions/deposit")
    @PreAuthorize("hasRole('CLIENT')")
    public Transaction deposit(
            @RequestParam Long clientId,
            @RequestParam BigDecimal amount) {
        ClientIdContextHolder.requireClient(clientId);

        return transactionsService.deposit(clientId, amount);
    }

    @PostMapping("/transactions/withdrawal")
    @PreAuthorize("hasRole('CLIENT')")
    public Transaction withdrawal(
            @RequestParam Long clientId,
            @RequestParam BigDecimal amount) {
        ClientIdContextHolder.requireClient(clientId);

        return transactionsService.withdrawal(clientId, amount);
    }

    @PostMapping("/transactions/purchase")
    @PreAuthorize("hasRole('CLIENT')")
    public Transaction purchase(
            @RequestParam Long clientId,
            @RequestParam BigDecimal amount) {
        ClientIdContextHolder.requireClient(clientId);

        return transactionsService.purchase(clientId, amount);
    }
}
