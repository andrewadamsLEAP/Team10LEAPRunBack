package com.example.repositories;

import com.example.entities.Transaction;
import com.example.mappers.TransactionsMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionsRepositoryTest {

    @Mock
    private TransactionsMapper transactionsMapper;

    @InjectMocks
    private TransactionsRepository transactionsRepository;

    /**
     * Tests getting transactions for a client
     */
    @Test
    void getTransactions_shouldReturnTransactionsForClient() {
        Long clientId = 1L;
        List<Transaction> mockTransactions = new ArrayList<>();
        
        Transaction tx = new Transaction();
        tx.setClientId(clientId);
        tx.setAmount(new BigDecimal("1000.00"));
        tx.setType("DEPOSIT");
        mockTransactions.add(tx);

        when(transactionsMapper.getTransactions(clientId)).thenReturn(mockTransactions);

        List<Transaction> result = transactionsRepository.getTransactions(clientId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(clientId, result.get(0).getClientId());
        verify(transactionsMapper, times(1)).getTransactions(clientId);
    }

    /**
     * Tests increasing buying power
     */
    @Test
    void increaseBuyingPower_shouldCallMapperAndReturnAffectedRows() {
        Long clientId = 1L;
        BigDecimal amount = new BigDecimal("500.00");

        when(transactionsMapper.increaseBuyingPower(clientId, amount)).thenReturn(1);

        int result = transactionsRepository.increaseBuyingPower(clientId, amount);

        assertEquals(1, result);
        verify(transactionsMapper, times(1)).increaseBuyingPower(clientId, amount);
    }

    /**
     * Tests decreasing buying power
     */
    @Test
    void decreaseBuyingPower_shouldCallMapperAndReturnAffectedRows() {
        Long clientId = 1L;
        BigDecimal amount = new BigDecimal("250.00");

        when(transactionsMapper.decreaseBuyingPower(clientId, amount)).thenReturn(1);

        int result = transactionsRepository.decreaseBuyingPower(clientId, amount);

        assertEquals(1, result);
        verify(transactionsMapper, times(1)).decreaseBuyingPower(clientId, amount);
    }

    /**
     * Tests saving transaction
     */
    @Test
    void saveTransaction_shouldCallMapperAndReturnAffectedRows() {
        Transaction tx = new Transaction();
        tx.setClientId(1L);
        tx.setAmount(new BigDecimal("1000.00"));
        tx.setType("DEPOSIT");

        when(transactionsMapper.saveTransaction(tx)).thenReturn(1);

        int result = transactionsRepository.saveTransaction(tx);

        assertEquals(1, result);
        verify(transactionsMapper, times(1)).saveTransaction(tx);
    }

    /**
     * Tests getting empty transactions list
     */
    @Test
    void getTransactions_shouldReturnEmptyListWhenNoTransactions() {
        Long clientId = 1L;

        when(transactionsMapper.getTransactions(clientId)).thenReturn(new ArrayList<>());

        List<Transaction> result = transactionsRepository.getTransactions(clientId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Tests multiple transactions for same client
     */
    @Test
    void getTransactions_shouldReturnMultipleTransactionsForClient() {
        Long clientId = 1L;
        List<Transaction> mockTransactions = new ArrayList<>();
        
        Transaction tx1 = new Transaction();
        tx1.setClientId(clientId);
        tx1.setType("DEPOSIT");
        mockTransactions.add(tx1);

        Transaction tx2 = new Transaction();
        tx2.setClientId(clientId);
        tx2.setType("WITHDRAWAL");
        mockTransactions.add(tx2);

        when(transactionsMapper.getTransactions(clientId)).thenReturn(mockTransactions);

        List<Transaction> result = transactionsRepository.getTransactions(clientId);

        assertEquals(2, result.size());
    }

    /**
     * Tests increasing buying power with large amount
     */
    @Test
    void increaseBuyingPower_shouldHandleLargeAmounts() {
        Long clientId = 1L;
        BigDecimal amount = new BigDecimal("999999.99");

        when(transactionsMapper.increaseBuyingPower(clientId, amount)).thenReturn(1);

        int result = transactionsRepository.increaseBuyingPower(clientId, amount);

        assertEquals(1, result);
    }

    /**
     * Tests decreasing buying power fails when amount too large
     */
    @Test
    void decreaseBuyingPower_shouldReturnZeroWhenInsufficientFunds() {
        Long clientId = 1L;
        BigDecimal amount = new BigDecimal("999999.99");

        when(transactionsMapper.decreaseBuyingPower(clientId, amount)).thenReturn(0);

        int result = transactionsRepository.decreaseBuyingPower(clientId, amount);

        assertEquals(0, result);
    }

    /**
     * Tests saving transaction with null transaction
     */
    @Test
    void saveTransaction_shouldHandleSavingTransaction() {
        Transaction tx = new Transaction();

        when(transactionsMapper.saveTransaction(tx)).thenReturn(1);

        int result = transactionsRepository.saveTransaction(tx);

        assertEquals(1, result);
    }

    /**
     * Tests getting transactions for different clients
     */
    @Test
    void getTransactions_shouldReturnDifferentTransactionsForDifferentClients() {
        List<Transaction> mockTx1 = new ArrayList<>();
        Transaction tx1 = new Transaction();
        tx1.setClientId(1L);
        mockTx1.add(tx1);

        List<Transaction> mockTx2 = new ArrayList<>();
        Transaction tx2 = new Transaction();
        tx2.setClientId(2L);
        mockTx2.add(tx2);

        when(transactionsMapper.getTransactions(1L)).thenReturn(mockTx1);
        when(transactionsMapper.getTransactions(2L)).thenReturn(mockTx2);

        List<Transaction> result1 = transactionsRepository.getTransactions(1L);
        List<Transaction> result2 = transactionsRepository.getTransactions(2L);

        assertEquals(1L, result1.get(0).getClientId());
        assertEquals(2L, result2.get(0).getClientId());
    }
}
