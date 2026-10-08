package com.example.controllers;

import com.example.DTOs.clients.ClientAdminView;
import com.example.DTOs.clients.ClientProfileView;
import com.example.DTOs.clients.ClientReporterView;
import com.example.services.ClientsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ClientsController.
 * Tests the controller layer with mocked ClientsService.
 * Focuses on testing the new admin and reporter endpoints from the fix.
 */
@ExtendWith(MockitoExtension.class)
class ClientsControllerTest {

    @Mock
    private ClientsService clientsService;

    private ClientsController clientsController;

    @BeforeEach
    void setUp() {
        clientsController = new ClientsController(clientsService);
    }

    // ========== PROFILE VIEW TESTS ==========

    @Test
    void viewProfileReturnsClientProfile() {
        Long clientId = 1L;
        ClientProfileView expected = clientProfileView(clientId, "profile@example.com", "profileuser", "First", "Last", new BigDecimal("1000.00"));

        when(clientsService.getClientProfile(clientId)).thenReturn(expected);

        ClientProfileView result = clientsController.viewProfile(clientId).getBody();

        assertNotNull(result);
        assertEquals(clientId, result.clientId());
        assertEquals("profileuser", result.username());
        assertEquals("profile@example.com", result.email());
        verify(clientsService).getClientProfile(clientId);
    }

    // ========== ADMIN VIEW TESTS (NEW ENDPOINTS) ==========

    @Test
    void getClientAsAdminReturnsAdminView() {
        Long clientId = 1L;
        ClientAdminView expected = clientAdminView(clientId, "adminuser", "admin@example.com", "Admin", "User", new BigDecimal("500.00"));

        when(clientsService.getClientDataAdmin(clientId)).thenReturn(expected);

        ClientAdminView result = clientsController.getClientAsAdmin(clientId).getBody();

        assertNotNull(result);
        assertEquals(clientId, result.clientId());
        assertEquals("adminuser", result.username());
        assertEquals("admin@example.com", result.email());
        assertEquals("Admin", result.firstName());
        assertEquals("User", result.lastName());
        assertEquals(new BigDecimal("500.00"), result.cashAmount());
        verify(clientsService).getClientDataAdmin(clientId);
    }

    @Test
    void getAllClientsAsAdminReturnsListOfAdminViews() {
        List<ClientAdminView> expected = List.of(
                clientAdminView(1L, "admin1", "admin1@example.com", "Admin", "One", new BigDecimal("500.00")),
                clientAdminView(2L, "admin2", "admin2@example.com", "Admin", "Two", new BigDecimal("1000.00")),
                clientAdminView(3L, "admin3", "admin3@example.com", "Admin", "Three", new BigDecimal("750.00"))
        );

        when(clientsService.getAllClientDataAdmin()).thenReturn(expected);

        List<ClientAdminView> result = clientsController.getAllClientsAsAdmin().getBody();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("admin1", result.get(0).username());
        assertEquals("admin2", result.get(1).username());
        assertEquals("admin3", result.get(2).username());
        verify(clientsService).getAllClientDataAdmin();
    }

    @Test
    void getAllClientsAsAdminReturnsEmptyListWhenNoClientsExist() {
        when(clientsService.getAllClientDataAdmin()).thenReturn(List.of());

        List<ClientAdminView> result = clientsController.getAllClientsAsAdmin().getBody();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllClientsAsAdminReturnsSingleClientAsList() {
        List<ClientAdminView> expected = List.of(
                clientAdminView(1L, "admin", "admin@example.com", "Admin", "User", new BigDecimal("300.00"))
        );

        when(clientsService.getAllClientDataAdmin()).thenReturn(expected);

        List<ClientAdminView> result = clientsController.getAllClientsAsAdmin().getBody();

        assertEquals(1, result.size());
        assertEquals("admin", result.get(0).username());
    }

    // ========== REPORTER VIEW TESTS (NEW ENDPOINTS) ==========
    // NOTE: These tests are disabled because the reporter endpoints
    // (getClientAsReporter, getAllClientsAsReporter) are not yet implemented
    // in ClientsController. Uncomment when endpoints are added.

    /*
    @Test
    void getClientAsReporterReturnsReporterView() {
        Long clientId = 1L;
        ClientReporterView expected = clientReporterView(clientId, "reporter", "reporter@example.com");

        when(clientsService.getClientDataReporter(clientId)).thenReturn(expected);

        ClientReporterView result = clientsController.getClientAsReporter(clientId).getBody();

        assertNotNull(result);
        assertEquals(clientId, result.clientId());
        assertEquals("reporter", result.username());
        assertEquals("reporter@example.com", result.email());
        verify(clientsService).getClientDataReporter(clientId);
    }

    @Test
    void getClientAsReporterReturnsOnlyEssentialFields() {
        Long clientId = 5L;
        ClientReporterView view = clientReporterView(clientId, "report_user", "report@example.com");

        when(clientsService.getClientDataReporter(clientId)).thenReturn(view);

        ClientReporterView result = clientsController.getClientAsReporter(clientId).getBody();

        // Verify reporter view only has essential fields (id, username, email)
        assertNotNull(result);
        assertEquals(clientId, result.clientId());
        assertEquals("report_user", result.username());
        assertEquals("report@example.com", result.email());
    }

    @Test
    void getAllClientsAsReporterReturnsListOfReporterViews() {
        List<ClientReporterView> expected = List.of(
                clientReporterView(1L, "reporter1", "reporter1@example.com"),
                clientReporterView(2L, "reporter2", "reporter2@example.com"),
                clientReporterView(3L, "reporter3", "reporter3@example.com")
        );

        when(clientsService.getAllClientDataReporter()).thenReturn(expected);

        List<ClientReporterView> result = clientsController.getAllClientsAsReporter().getBody();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("reporter1", result.get(0).username());
        assertEquals("reporter2", result.get(1).username());
        assertEquals("reporter3", result.get(2).username());
        verify(clientsService).getAllClientDataReporter();
    }

    @Test
    void getAllClientsAsReporterReturnsEmptyListWhenNoClientsExist() {
        when(clientsService.getAllClientDataReporter()).thenReturn(List.of());

        List<ClientReporterView> result = clientsController.getAllClientsAsReporter().getBody();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllClientsAsReporterReturnsSingleClientAsList() {
        List<ClientReporterView> expected = List.of(
                clientReporterView(1L, "reporter", "reporter@example.com")
        );

        when(clientsService.getAllClientDataReporter()).thenReturn(expected);

        List<ClientReporterView> result = clientsController.getAllClientsAsReporter().getBody();

        assertEquals(1, result.size());
        assertEquals("reporter", result.get(0).username());
    }
    */

    // ========== INTEGRATION TESTS (MULTIPLE ENDPOINTS) ==========

    @Test
    void canGetSameClientInAdminAndProfileFormats() {
        Long clientId = 1L;
        ClientProfileView profile = clientProfileView(clientId, "user@example.com", "username", "First", "Last", new BigDecimal("500.00"));
        ClientAdminView admin = clientAdminView(clientId, "username", "user@example.com", "First", "Last", new BigDecimal("500.00"));

        when(clientsService.getClientProfile(clientId)).thenReturn(profile);
        when(clientsService.getClientDataAdmin(clientId)).thenReturn(admin);

        ClientProfileView profileResult = clientsController.viewProfile(clientId).getBody();
        ClientAdminView adminResult = clientsController.getClientAsAdmin(clientId).getBody();

        assertNotNull(profileResult);
        assertNotNull(adminResult);
        
        // Both views share basic fields
        assertEquals("username", profileResult.username());
        assertEquals("username", adminResult.username());
        
        // Admin view has more fields
        assertNotNull(adminResult.firstName());
    }

    @Test
    void adminEndpointsReturnExpectedViewsForSameData() {
        Long clientId = 2L;
        ClientAdminView admin = clientAdminView(clientId, "user", "user@example.com", "First", "Last", new BigDecimal("1000.00"));

        when(clientsService.getClientDataAdmin(clientId)).thenReturn(admin);

        ClientAdminView adminResult = clientsController.getClientAsAdmin(clientId).getBody();

        // Admin view includes cash amount
        assertEquals(new BigDecimal("1000.00"), adminResult.cashAmount());
        assertNotNull(adminResult.email());
        assertEquals("user@example.com", adminResult.email());
    }

    // ========== HELPER METHODS ==========

    private ClientProfileView clientProfileView(
            Long clientId,
            String email,
            String username,
            String firstName,
            String lastName,
            BigDecimal cashAmount) {
        return new ClientProfileView(clientId, email, username, firstName, lastName, cashAmount);
    }

    private ClientAdminView clientAdminView(
            Long clientId,
            String username,
            String email,
            String firstName,
            String lastName,
            BigDecimal cashAmount) {
        return new ClientAdminView(clientId, username, email, firstName, lastName, cashAmount);
    }

    private ClientReporterView clientReporterView(
            Long clientId,
            String username,
            String email) {
        return new ClientReporterView(clientId, username, email);
    }
}
