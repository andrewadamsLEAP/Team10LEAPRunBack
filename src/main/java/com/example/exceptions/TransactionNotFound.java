package com.example.exceptions;


/**
 * Exception thrown when a transaction with a specified ID is not found.
 * Used in TransactionService when a transaction cannot be found by its ID.
 */

public class TransactionNotFound extends RuntimeException {
    public TransactionNotFound(String message) {
        super(message);
    }

    public TransactionNotFound(Long transactionId) {
        super("Transaction with ID " + transactionId + " not found");
    }
}