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

    @GetMapping("/admin/transactions")
    @PreAuthorize("hasRole('EMPLOYEE_ADMIN')")
    public List<Transaction> getAllTransactionsForAdmin() {
        return transactionsService.getAllTransactions();
    }

    @GetMapping("/admin/transactions/{clientId}")
    @PreAuthorize("hasRole('EMPLOYEE_ADMIN')")
    public List<Transaction> getTransactionsForClientAsAdmin(
            @PathVariable Long clientId) {
        return transactionsService.getTransactions(clientId);
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasRole('CLIENT')")
    public List<Transaction> getTransactions() {
        Long clientId = ClientIdContextHolder.requireClientId();
        return transactionsService.getTransactions(clientId);
    }

    @PostMapping("/transactions/deposit")
    @PreAuthorize("hasRole('CLIENT')")
    public Transaction deposit(
            @RequestParam BigDecimal amount) {
        Long clientId = ClientIdContextHolder.requireClientId();
        return transactionsService.deposit(clientId, amount);
    }

    @PostMapping("/transactions/withdrawal")
    @PreAuthorize("hasRole('CLIENT')")
    public Transaction withdrawal(
            @RequestParam BigDecimal amount) {
        Long clientId = ClientIdContextHolder.requireClientId();
        return transactionsService.withdrawal(clientId, amount);
    }

    @PostMapping("/transactions/purchase")
    @PreAuthorize("hasRole('CLIENT')")
    public Transaction purchase(
            @RequestParam BigDecimal amount) {
        Long clientId = ClientIdContextHolder.requireClientId();
        return transactionsService.purchase(clientId, amount);
    }
}
