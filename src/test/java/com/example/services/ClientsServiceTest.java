package com.example.services;

import com.example.DTOs.clients.ClientAdminView;
import com.example.DTOs.clients.ClientProfileUpdateRequest;
import com.example.DTOs.clients.ClientProfileView;
import com.example.DTOs.clients.ClientReporterView;
import com.example.entities.Client;
import com.example.exceptions.UserNotFoundException;
import com.example.repositories.ClientsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ClientsServiceTest {
    private ClientsRepository clientsRepository;
    private ClientsService clientsService;

    @BeforeEach
    void setUp() {
        clientsRepository = mock(ClientsRepository.class);
        clientsService = new ClientsService(clientsRepository);
    }

    @Test
    void getClientProfileReturnsMappedView() {
        when(clientsRepository.findClientById(7L)).thenReturn(client(7L));

        ClientProfileView result = clientsService.getClientProfile(7L);

        assertEquals(7L, result.clientId());
        assertEquals("client@example.com", result.email());
        assertEquals("client", result.username());
    }

    @Test
    void getClientDataAdminReturnsView() {
        ClientAdminView expected = new ClientAdminView(1L, "client", "client@example.com", "First", "Last", BigDecimal.TEN);
        when(clientsRepository.findClientByIdAdmin(1L)).thenReturn(expected);

        assertEquals(expected, clientsService.getClientDataAdmin(1L));
    }

    @Test
    void getClientDataReporterReturnsView() {
        ClientReporterView expected = new ClientReporterView(1L, "client", "client@example.com");
        when(clientsRepository.findClientByIdReporter(1L)).thenReturn(expected);

        assertEquals(expected, clientsService.getClientDataReporter(1L));
    }

    @Test
    void getAllClientDataAdminReturnsResults() {
        List<ClientAdminView> expected = List.of(
                new ClientAdminView(1L, "client", "client@example.com", "First", "Last", BigDecimal.TEN));
        when(clientsRepository.findClientsAsAdmin()).thenReturn(expected);

        assertEquals(expected, clientsService.getAllClientDataAdmin());
    }

    @Test
    void getAllClientDataReporterReturnsResults() {
        List<ClientReporterView> expected = List.of(
                new ClientReporterView(1L, "client", "client@example.com"));
        when(clientsRepository.findClientsAsReporter()).thenReturn(expected);

        assertEquals(expected, clientsService.getAllClientDataReporter());
    }

    @Test
    void updateProfileDelegatesToRepository() {
        ClientProfileUpdateRequest request = new ClientProfileUpdateRequest("updated", "New", "Name");
        when(clientsRepository.findClientById(1L)).thenReturn(client(1L));
        when(clientsRepository.findClientIdByUsername("updated")).thenReturn(null);
        when(clientsRepository.updateProfile(1L, request)).thenReturn(1);

        clientsService.updateProfile(1L, request);

        verify(clientsRepository).updateProfile(1L, request);
    }

    @Test
    void getClientProfileThrowsWhenMissing() {
        when(clientsRepository.findClientById(99L)).thenReturn(null);

        assertThrows(UserNotFoundException.class, () -> clientsService.getClientProfile(99L));
    }

    private Client client(Long id) {
        Client client = new Client();
        client.setClientId(id);
        client.setEmail("client@example.com");
        client.setUsername("client");
        client.setFirstName("First");
        client.setLastName("Last");
        client.setCashAmount(BigDecimal.TEN);
        return client;
    }
}
