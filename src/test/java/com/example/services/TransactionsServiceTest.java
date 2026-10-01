package com.example.services;

import com.example.DTOs.clients.ClientProfileView;
import com.example.entities.Transaction;
import com.example.entities.TransactionType;
import com.example.repositories.TransactionsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionsServiceTest {

    private TransactionsRepository transactionsRepository;
    private ClientsService clientsService;
    private TransactionsService transactionsService;

    @BeforeEach
    void setUp() {
        transactionsRepository = mock(TransactionsRepository.class);
        clientsService = mock(ClientsService.class);
        transactionsService = new TransactionsService(transactionsRepository, clientsService);
    }

    // ========== DEPOSIT TESTS ==========

    @Test
    void depositCreatesTransactionWhenClientExistsAndAmountIsValid() {
        Long clientId = 1L;
        BigDecimal amount = new BigDecimal("500.00");
        
        ClientProfileView clientProfile = clientProfile(clientId, "test@example.com", "testuser");
        when(clientsService.getClientProfile(clientId)).thenReturn(clientProfile);
        when(transactionsRepository.increaseBuyingPower(clientId, amount)).thenReturn(1);
        when(transactionsRepository.saveTransaction(any(Transaction.class))).thenReturn(1);

        Transaction result = transactionsService.deposit(clientId, amount);

        assertNotNull(result);
        assertEquals(TransactionType.DEPOSIT.name(), result.getType());
        assertEquals(amount, result.getAmount());
        assertEquals(clientId, result.getClientId());
        verify(clientsService).getClientProfile(clientId);
        verify(transactionsRepository).increaseBuyingPower(clientId, amount);
        verify(transactionsRepository).saveTransaction(any(Transaction.class));
    }

    @Test
    void depositThrowsWhenClientDoesNotExist() {
        Long invalidClientId = 999L;
        BigDecimal amount = new BigDecimal("500.00");
        
        when(clientsService.getClientProfile(invalidClientId))
                .thenThrow(new IllegalArgumentException("Client not found"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionsService.deposit(invalidClientId, amount)
        );

        assertEquals("Client not found: 999", exception.getMessage());
        verify(transactionsRepository, never()).increaseBuyingPower(anyLong(), any());
        verify(transactionsRepository, never()).saveTransaction(any());
    }

    @Test
    void depositThrowsWhenAmountIsNegative() {
        Long clientId = 1L;
        BigDecimal negativeAmount = new BigDecimal("-100.00");
        
        ClientProfileView clientProfile = clientProfile(clientId, "test@example.com", "testuser");
        when(clientsService.getClientProfile(clientId)).thenReturn(clientProfile);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionsService.deposit(clientId, negativeAmount)
        );

        assertEquals("Amount must be greater than zero", exception.getMessage());
        verify(transactionsRepository, never()).increaseBuyingPower(anyLong(), any());
    }

    @Test
    void depositThrowsWhenAmountIsZero() {
        Long clientId = 1L;
        BigDecimal zeroAmount = BigDecimal.ZERO;
        
        ClientProfileView clientProfile = clientProfile(clientId, "test@example.com", "testuser");
        when(clientsService.getClientProfile(clientId)).thenReturn(clientProfile);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionsService.deposit(clientId, zeroAmount)
        );

        assertEquals("Amount must be greater than zero", exception.getMessage());
    }

    @Test
    void depositThrowsWhenClientIdIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionsService.deposit(null, new BigDecimal("500.00"))
        );

        assertEquals("Client ID must be greater than zero", exception.getMessage());
        verify(clientsService, never()).getClientProfile(any());
    }

    @Test
    void depositThrowsWhenClientIdIsZero() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionsService.deposit(0L, new BigDecimal("500.00"))
        );

        assertEquals("Client ID must be greater than zero", exception.getMessage());
    }

    @Test
    void depositThrowsWhenAmountIsNull() {
        Long clientId = 1L;
        ClientProfileView clientProfile = clientProfile(clientId, "test@example.com", "testuser");
        when(clientsService.getClientProfile(clientId)).thenReturn(clientProfile);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionsService.deposit(clientId, null)
        );

        assertEquals("Amount must be greater than zero", exception.getMessage());
    }

    // ========== WITHDRAWAL TESTS ==========

    @Test
    void withdrawalCreatesTransactionWhenClientExistsAndAmountIsValid() {
        Long clientId = 1L;
        BigDecimal amount = new BigDecimal("200.00");
        
        ClientProfileView clientProfile = clientProfile(clientId, "test@example.com", "testuser");
        when(clientsService.getClientProfile(clientId)).thenReturn(clientProfile);
        when(transactionsRepository.decreaseBuyingPower(clientId, amount)).thenReturn(1);
        when(transactionsRepository.saveTransaction(any(Transaction.class))).thenReturn(1);

        Transaction result = transactionsService.withdrawal(clientId, amount);

        assertNotNull(result);
        assertEquals(TransactionType.WITHDRAWAL.name(), result.getType());
        assertEquals(amount, result.getAmount());
        assertEquals(clientId, result.getClientId());
        verify(clientsService).getClientProfile(clientId);
        verify(transactionsRepository).decreaseBuyingPower(clientId, amount);
    }

    @Test
    void withdrawalThrowsWhenClientDoesNotExist() {
        Long invalidClientId = 999L;
        BigDecimal amount = new BigDecimal("200.00");
        
        when(clientsService.getClientProfile(invalidClientId))
                .thenThrow(new IllegalArgumentException("Client not found"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionsService.withdrawal(invalidClientId, amount)
        );

        assertEquals("Client not found: 999", exception.getMessage());
        verify(transactionsRepository, never()).decreaseBuyingPower(anyLong(), any());
    }

    @Test
    void withdrawalThrowsWhenAmountIsNegative() {
        Long clientId = 1L;
        BigDecimal negativeAmount = new BigDecimal("-50.00");
        
        ClientProfileView clientProfile = clientProfile(clientId, "test@example.com", "testuser");
        when(clientsService.getClientProfile(clientId)).thenReturn(clientProfile);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionsService.withdrawal(clientId, negativeAmount)
        );

        assertEquals("Amount must be greater than zero", exception.getMessage());
    }

    // ========== PURCHASE TESTS ==========

    @Test
    void purchaseCreatesTransactionWhenClientExistsAndAmountIsValid() {
        Long clientId = 1L;
        BigDecimal amount = new BigDecimal("1500.00");
        
        ClientProfileView clientProfile = clientProfile(clientId, "test@example.com", "testuser");
        when(clientsService.getClientProfile(clientId)).thenReturn(clientProfile);
        when(transactionsRepository.decreaseBuyingPower(clientId, amount)).thenReturn(1);
        when(transactionsRepository.saveTransaction(any(Transaction.class))).thenReturn(1);

        Transaction result = transactionsService.purchase(clientId, amount);

        assertNotNull(result);
        assertEquals(TransactionType.PURCHASE.name(), result.getType());
        assertEquals(amount, result.getAmount());
        assertEquals(clientId, result.getClientId());
        verify(clientsService).getClientProfile(clientId);
        verify(transactionsRepository).decreaseBuyingPower(clientId, amount);
    }

    @Test
    void purchaseThrowsWhenClientDoesNotExist() {
        Long invalidClientId = 999L;
        BigDecimal amount = new BigDecimal("1500.00");
        
        when(clientsService.getClientProfile(invalidClientId))
                .thenThrow(new IllegalArgumentException("Client not found"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionsService.purchase(invalidClientId, amount)
        );

        assertEquals("Client not found: 999", exception.getMessage());
    }

    @Test
    void purchaseThrowsWhenAmountIsNegative() {
        Long clientId = 1L;
        BigDecimal negativeAmount = new BigDecimal("-1500.00");
        
        ClientProfileView clientProfile = clientProfile(clientId, "test@example.com", "testuser");
        when(clientsService.getClientProfile(clientId)).thenReturn(clientProfile);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionsService.purchase(clientId, negativeAmount)
        );

        assertEquals("Amount must be greater than zero", exception.getMessage());
    }

    // ========== GET TRANSACTIONS TESTS ==========

    @Test
    void getTransactionsReturnsListForValidClient() {
        Long clientId = 1L;
        Transaction transaction1 = transaction(1L, clientId, TransactionType.DEPOSIT.name(), new BigDecimal("500.00"));
        Transaction transaction2 = transaction(2L, clientId, TransactionType.PURCHASE.name(), new BigDecimal("1500.00"));
        List<Transaction> expected = List.of(transaction1, transaction2);
        
        when(transactionsRepository.getTransactions(clientId)).thenReturn(expected);

        List<Transaction> result = transactionsService.getTransactions(clientId);

        assertEquals(2, result.size());
        assertEquals(expected, result);
        verify(transactionsRepository).getTransactions(clientId);
    }

    @Test
    void getTransactionsReturnsEmptyListForClientWithNoTransactions() {
        Long clientId = 1L;
        when(transactionsRepository.getTransactions(clientId)).thenReturn(List.of());

        List<Transaction> result = transactionsService.getTransactions(clientId);

        assertTrue(result.isEmpty());
    }

    // ========== TRANSACTION TYPE VALIDATION TESTS ==========

    @Test
    void allTransactionTypesUseEnums() {
        Long clientId = 1L;
        ClientProfileView clientProfile = clientProfile(clientId, "test@example.com", "testuser");
        when(clientsService.getClientProfile(clientId)).thenReturn(clientProfile);
        when(transactionsRepository.increaseBuyingPower(anyLong(), any())).thenReturn(1);
        when(transactionsRepository.decreaseBuyingPower(anyLong(), any())).thenReturn(1);
        when(transactionsRepository.saveTransaction(any(Transaction.class))).thenReturn(1);

        Transaction depositTx = transactionsService.deposit(clientId, new BigDecimal("100.00"));
        Transaction withdrawalTx = transactionsService.withdrawal(clientId, new BigDecimal("50.00"));
        Transaction purchaseTx = transactionsService.purchase(clientId, new BigDecimal("200.00"));

        assertEquals(TransactionType.DEPOSIT.name(), depositTx.getType());
        assertEquals(TransactionType.WITHDRAWAL.name(), withdrawalTx.getType());
        assertEquals(TransactionType.PURCHASE.name(), purchaseTx.getType());
    }

    // ========== HELPER METHODS ==========

    private ClientProfileView clientProfile(Long clientId, String email, String username) {
        return new ClientProfileView(clientId, email, username, "First", "Last", new BigDecimal("1000.00"));
    }

    private Transaction transaction(Long transactionId, Long clientId, String type, BigDecimal amount) {
        return new Transaction(transactionId, clientId, type, amount, null);
    }
}
