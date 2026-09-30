package com.example.transactions;

import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class TransactionsRepository {

    private final TransactionsMapper transactionsMapper;

    public TransactionsRepository(
            TransactionsMapper transactionsMapper) {

        this.transactionsMapper = transactionsMapper;
    }

    public List<Transaction> getTransactions(Long clientId) {
        return transactionsMapper.getTransactions(clientId);
    }

    public int increaseBuyingPower(
            Long clientId,
            BigDecimal amount) {

        return transactionsMapper.increaseBuyingPower(
                clientId,
                amount
        );
    }

    public int decreaseBuyingPower(
            Long clientId,
            BigDecimal amount) {

        return transactionsMapper.decreaseBuyingPower(
                clientId,
                amount
        );
    }

    public int saveTransaction(Transaction transaction) {
        return transactionsMapper.saveTransaction(transaction);
    }
}
