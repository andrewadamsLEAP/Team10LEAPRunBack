package com.example.controllers;

import com.example.entities.Transaction;
import com.example.services.TransactionsService;
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
class TransactionsControllerTest {

    @Mock
    private TransactionsService transactionsService;

    @InjectMocks
    private TransactionsController transactionsController;

    /**
     * Tests getting transactions for a client
     */
    @Test
    void getTransactions_shouldReturnTransactionsForClient() {
        Long clientId = 1L;
        List<Transaction> mockTransactions = new ArrayList<>();
        
        Transaction tx1 = new Transaction();
        tx1.setClientId(clientId);
        tx1.setAmount(new BigDecimal("1000.00"));
        mockTransactions.add(tx1);

        when(transactionsService.getTransactions(clientId)).thenReturn(mockTransactions);

        List<Transaction> result = transactionsController.getTransactions(clientId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(clientId, result.get(0).getClientId());
        verify(transactionsService, times(1)).getTransactions(clientId);
    }

    /**
     * Tests deposit transaction
     */
    @Test
    void deposit_shouldCallServiceAndReturnTransaction() {
        Long clientId = 1L;
        BigDecimal amount = new BigDecimal("500.00");

        Transaction mockTx = new Transaction();
        mockTx.setClientId(clientId);
        mockTx.setAmount(amount);
        mockTx.setType("DEPOSIT");

        when(transactionsService.deposit(clientId, amount)).thenReturn(mockTx);

        Transaction result = transactionsController.deposit(clientId, amount);

        assertNotNull(result);
        assertEquals(clientId, result.getClientId());
        assertEquals(amount, result.getAmount());
        assertEquals("DEPOSIT", result.getType());
        verify(transactionsService, times(1)).deposit(clientId, amount);
    }

    /**
     * Tests withdrawal transaction
     */
    @Test
    void withdrawal_shouldCallServiceAndReturnTransaction() {
        Long clientId = 1L;
        BigDecimal amount = new BigDecimal("250.00");

        Transaction mockTx = new Transaction();
        mockTx.setClientId(clientId);
        mockTx.setAmount(amount);
        mockTx.setType("WITHDRAWAL");

        when(transactionsService.withdrawal(clientId, amount)).thenReturn(mockTx);

        Transaction result = transactionsController.withdrawal(clientId, amount);

        assertNotNull(result);
        assertEquals("WITHDRAWAL", result.getType());
        verify(transactionsService, times(1)).withdrawal(clientId, amount);
    }

    /**
     * Tests purchase transaction
     */
    @Test
    void purchase_shouldCallServiceAndReturnTransaction() {
        Long clientId = 1L;
        BigDecimal amount = new BigDecimal("1000.00");

        Transaction mockTx = new Transaction();
        mockTx.setClientId(clientId);
        mockTx.setAmount(amount);
        mockTx.setType("PURCHASE");

        when(transactionsService.purchase(clientId, amount)).thenReturn(mockTx);

        Transaction result = transactionsController.purchase(clientId, amount);

        assertNotNull(result);
        assertEquals("PURCHASE", result.getType());
        verify(transactionsService, times(1)).purchase(clientId, amount);
    }

    /**
     * Tests getting empty transaction list
     */
    @Test
    void getTransactions_shouldReturnEmptyListWhenNoTransactions() {
        Long clientId = 1L;

        when(transactionsService.getTransactions(clientId)).thenReturn(new ArrayList<>());

        List<Transaction> result = transactionsController.getTransactions(clientId);

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
        tx1.setAmount(new BigDecimal("1000.00"));
        tx1.setType("DEPOSIT");
        mockTransactions.add(tx1);

        Transaction tx2 = new Transaction();
        tx2.setClientId(clientId);
        tx2.setAmount(new BigDecimal("500.00"));
        tx2.setType("WITHDRAWAL");
        mockTransactions.add(tx2);

        when(transactionsService.getTransactions(clientId)).thenReturn(mockTransactions);

        List<Transaction> result = transactionsController.getTransactions(clientId);

        assertEquals(2, result.size());
    }

    /**
     * Tests deposit with large amount
     */
    @Test
    void deposit_shouldHandleLargeAmounts() {
        Long clientId = 1L;
        BigDecimal amount = new BigDecimal("999999.99");

        Transaction mockTx = new Transaction();
        mockTx.setAmount(amount);

        when(transactionsService.deposit(clientId, amount)).thenReturn(mockTx);

        Transaction result = transactionsController.deposit(clientId, amount);

        assertEquals(amount, result.getAmount());
    }
}
