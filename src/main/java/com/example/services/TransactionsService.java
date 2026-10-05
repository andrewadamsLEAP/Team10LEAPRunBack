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

    @Transactional
    public Transaction deposit(
            Long clientId,
            BigDecimal amount) {
        logger.info("Deposit request: clientId={}, amount={}", clientId, amount);

        validateClientExists(clientId);
        validateAmount(amount);

        transactionsRepository.increaseBuyingPower(
                clientId,
                amount
        );

        Transaction transaction = new Transaction(
                null,
                clientId,
                TransactionType.DEPOSIT.name(),
                amount,
                null
        );

        transactionsRepository.saveTransaction(transaction);
        logger.info("Deposit completed: clientId={}, amount={}", clientId, amount);
        return transaction;
    }

    @Transactional
    public Transaction withdrawal(
            Long clientId,
            BigDecimal amount) {
        logger.info("Withdrawal request: clientId={}, amount={}", clientId, amount);

        validateClientExists(clientId);
        validateAmount(amount);

        transactionsRepository.decreaseBuyingPower(
                clientId,
                amount
        );

        Transaction transaction = new Transaction(
                null,
                clientId,
                TransactionType.WITHDRAWAL.name(),
                amount,
                null
        );

        transactionsRepository.saveTransaction(transaction);
        logger.info("Withdrawal completed: clientId={}, amount={}", clientId, amount);
        return transaction;
    }

    @Transactional
    public Transaction purchase(
            Long clientId,
            BigDecimal amount) {
        logger.info("Purchase transaction: clientId={}, amount={}", clientId, amount);

        validateClientExists(clientId);
        validateAmount(amount);

        transactionsRepository.decreaseBuyingPower(
                clientId,
                amount
        );

        Transaction transaction = new Transaction(
                null,
                clientId,
                TransactionType.PURCHASE.name(),
                amount,
                null
        );

        transactionsRepository.saveTransaction(transaction);

        return transaction;
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
    }
}
