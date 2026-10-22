package com.example.DTOs.Transactions;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Transaction response including the associated client's username.
 */
public record TransactionView(
        Long transactionId,
        Long clientId,
        String clientUsername,
        String type,
        BigDecimal amount,
        OffsetDateTime timestamp
) {}
