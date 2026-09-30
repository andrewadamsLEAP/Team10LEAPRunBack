package com.example.transactions;

import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/trading")
public class TransactionsController {

    private final TransactionsService transactionsService;

    public TransactionsController(TransactionsService transactionsService) {
        this.transactionsService = transactionsService;
    }

    @GetMapping("/transactions")
    public List<Transaction> getTransactions(
            @RequestParam Long clientId) {

        return transactionsService.getTransactions(clientId);
    }

    @PostMapping("/transactions/deposit")
    public Transaction deposit(
            @RequestParam Long clientId,
            @RequestParam BigDecimal amount) {

        return transactionsService.deposit(clientId, amount);
    }

    @PostMapping("/transactions/withdrawal")
    public Transaction withdrawal(
            @RequestParam Long clientId,
            @RequestParam BigDecimal amount) {

        return transactionsService.withdrawal(clientId, amount);
    }

    @PostMapping("/transactions/purchase")
    public Transaction purchase(
            @RequestParam Long clientId,
            @RequestParam BigDecimal amount) {

        return transactionsService.purchase(clientId, amount);
    }
}
