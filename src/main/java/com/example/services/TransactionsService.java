package com.example.services;

import com.example.entities.Transaction;
import com.example.entities.TransactionType;
import com.example.repositories.TransactionsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionsService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionsService.class);

    private final TransactionsRepository transactionsRepository;
    private final ClientsService clientsService;

    public TransactionsService(
            TransactionsRepository transactionsRepository,
            ClientsService clientsService) {

        this.transactionsRepository = transactionsRepository;
        this.clientsService = clientsService;
    }

    public List<Transaction> getTransactions(Long clientId) {
        return transactionsRepository.getTransactions(clientId);
    }

    /**
     * Execute a transaction (deposit, withdrawal, or purchase).
     * This is the core method that handles all transaction types.
     * 
     * @param clientId the ID of the client
     * @param amount the transaction amount
     * @param type the type of transaction (DEPOSIT, WITHDRAWAL, PURCHASE)
     * @return the created transaction
     */
    @Transactional
    public Transaction executeTransaction(Long clientId, BigDecimal amount, TransactionType type) {
        logger.info("{} request: clientId={}, amount={}", type, clientId, amount);

        validateClientExists(clientId);
        validateAmount(amount);

        // Apply operation based on type
        if (type == TransactionType.DEPOSIT) {
            transactionsRepository.increaseBuyingPower(clientId, amount);
        } else if (type == TransactionType.WITHDRAWAL || type == TransactionType.PURCHASE) {
            transactionsRepository.decreaseBuyingPower(clientId, amount);
        }

        // Create and save transaction
        Transaction transaction = new Transaction(
                null,
                clientId,
                type.name(),
                amount,
                null
        );

        transactionsRepository.saveTransaction(transaction);
        logger.info("{} completed: clientId={}, amount={}", type, clientId, amount);
        return transaction;
    }

    @Transactional
    public Transaction deposit(Long clientId, BigDecimal amount) {
        return executeTransaction(clientId, amount, TransactionType.DEPOSIT);
    }

    @Transactional
    public Transaction withdrawal(Long clientId, BigDecimal amount) {
        return executeTransaction(clientId, amount, TransactionType.WITHDRAWAL);
    }

    @Transactional
    public Transaction purchase(Long clientId, BigDecimal amount) {
        return executeTransaction(clientId, amount, TransactionType.PURCHASE);
    }

    private void validateClientExists(Long clientId) {
        if (clientId == null || clientId <= 0) {
            throw new IllegalArgumentException(
                    "Client ID must be greater than zero"
            );
        }
        
        try {
            clientsService.getClientProfile(clientId);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Client not found: " + clientId
            );
        }
    }

    private void validateAmount(BigDecimal amount) {

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Amount must be greater than zero"
            );
        }

        if (amount.compareTo(new BigDecimal("3000000")) > 0) {
            throw new IllegalArgumentException(
                    "Amount must not exceed 3,000,000"
            );
        }
    }
}
