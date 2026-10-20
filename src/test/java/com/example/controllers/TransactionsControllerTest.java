package com.example.controllers;

import com.example.authUtils.ClientIdContextHolder;
import com.example.entities.Transaction;
import com.example.services.TransactionsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionsControllerTest {
    private static final Long CLIENT_ID = 1L;

    @Mock
    private TransactionsService transactionsService;

    @InjectMocks
    private TransactionsController transactionsController;

    @BeforeEach
    void setUpContext() {
        ClientIdContextHolder.setClientId(CLIENT_ID);
    }

    @AfterEach
    void clearContext() {
        ClientIdContextHolder.clear();
    }

    @Test
    void getTransactionsUsesAuthenticatedClient() {
        List<Transaction> expected = List.of(new Transaction());
        when(transactionsService.getTransactions(CLIENT_ID)).thenReturn(expected);

        assertEquals(expected, transactionsController.getTransactions());
        verify(transactionsService).getTransactions(CLIENT_ID);
    }

    @Test
    void depositUsesAuthenticatedClient() {
        BigDecimal amount = new BigDecimal("500.00");
        Transaction expected = new Transaction();
        when(transactionsService.deposit(CLIENT_ID, amount)).thenReturn(expected);

        assertEquals(expected, transactionsController.deposit(amount));
        verify(transactionsService).deposit(CLIENT_ID, amount);
    }

    @Test
    void withdrawalUsesAuthenticatedClient() {
        BigDecimal amount = new BigDecimal("250.00");
        Transaction expected = new Transaction();
        when(transactionsService.withdrawal(CLIENT_ID, amount)).thenReturn(expected);

        assertEquals(expected, transactionsController.withdrawal(amount));
        verify(transactionsService).withdrawal(CLIENT_ID, amount);
    }

    @Test
    void purchaseUsesAuthenticatedClient() {
        BigDecimal amount = new BigDecimal("1000.00");
        Transaction expected = new Transaction();
        when(transactionsService.purchase(CLIENT_ID, amount)).thenReturn(expected);

        assertEquals(expected, transactionsController.purchase(amount));
        verify(transactionsService).purchase(CLIENT_ID, amount);
    }
}
