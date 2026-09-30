package com.example.services;

import com.example.repositories.TransactionsRepository;
import com.example.objects.Transaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionsService {

    private final TransactionsRepository transactionsRepository;

    public TransactionsService(
            TransactionsRepository transactionsRepository) {

        this.transactionsRepository = transactionsRepository;
    }

    public List<Transaction> getTransactions(Long clientId) {
        return transactionsRepository.getTransactions(clientId);
    }

    @Transactional
    public Transaction deposit(
            Long clientId,
            BigDecimal amount) {

        validateAmount(amount);

        transactionsRepository.increaseBuyingPower(
                clientId,
                amount
        );

        Transaction transaction = new Transaction(
                null,
                clientId,
                "DEPOSIT",
                amount,
                null
        );

        transactionsRepository.saveTransaction(transaction);

        return transaction;
    }

    @Transactional
    public Transaction withdrawal(
            Long clientId,
            BigDecimal amount) {

        validateAmount(amount);

        transactionsRepository.decreaseBuyingPower(
                clientId,
                amount
        );

        Transaction transaction = new Transaction(
                null,
                clientId,
                "WITHDRAWAL",
                amount,
                null
        );

        transactionsRepository.saveTransaction(transaction);

        return transaction;
    }

    @Transactional
    public Transaction purchase(
            Long clientId,
            BigDecimal amount) {

        validateAmount(amount);

        transactionsRepository.decreaseBuyingPower(
                clientId,
                amount
        );

        Transaction transaction = new Transaction(
                null,
                clientId,
                "PURCHASE",
                amount,
                null
        );

        transactionsRepository.saveTransaction(transaction);

        return transaction;
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
