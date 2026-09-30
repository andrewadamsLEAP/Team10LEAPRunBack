package com.example.repositories;

import com.example.DTOs.clients.ClientAdminView;
import com.example.DTOs.clients.ClientLoginView;
import com.example.DTOs.clients.ClientProfileUpdateRequest;
import com.example.DTOs.clients.ClientReporterView;
import com.example.entities.Client;
import com.example.mappers.ClientsMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientsRepositoryTest {

    @Test
    void findClientByIdDelegatesToMapper() {
        ClientsMapper mapper = mock(ClientsMapper.class);
        Client expected = client(1L, "client@example.com", "clientuser", "password1", "Client", "User", BigDecimal.ZERO);
        when(mapper.findById(1L)).thenReturn(expected);
        ClientsRepository repository = new ClientsRepository(mapper);

        Client actual = repository.findClientById(1L);

        assertSame(expected, actual);
        verify(mapper).findById(1L);
    }

    @Test
    void findClientIdByEmailDelegatesToMapper() {
        ClientsMapper mapper = mock(ClientsMapper.class);
        when(mapper.findClientIdByEmail("client@example.com")).thenReturn(1L);
        ClientsRepository repository = new ClientsRepository(mapper);

        Long actual = repository.findClientIdByEmail("client@example.com");

        assertEquals(1L, actual);
        verify(mapper).findClientIdByEmail("client@example.com");
    }

    @Test
    void findClientIdByUsernameDelegatesToMapper() {
        ClientsMapper mapper = mock(ClientsMapper.class);
        when(mapper.findClientIdByUsername("clientuser")).thenReturn(2L);
        ClientsRepository repository = new ClientsRepository(mapper);

        Long actual = repository.findClientIdByUsername("clientuser");

        assertEquals(2L, actual);
        verify(mapper).findClientIdByUsername("clientuser");
    }

    @Test
    void findClientByIdAdminDelegatesToMapper() {
        ClientsMapper mapper = mock(ClientsMapper.class);
        ClientAdminView expected = new ClientAdminView(3L, "admin", "admin@example.com", "Admin", "User", BigDecimal.ZERO);
        when(mapper.findByIdAdmin(3L)).thenReturn(expected);
        ClientsRepository repository = new ClientsRepository(mapper);

        ClientAdminView actual = repository.findClientByIdAdmin(3L);

        assertSame(expected, actual);
        verify(mapper).findByIdAdmin(3L);
    }

    @Test
    void findClientByIdReporterDelegatesToMapper() {
        ClientsMapper mapper = mock(ClientsMapper.class);
        ClientReporterView expected = new ClientReporterView(4L, "reporter", "reporter@example.com");
        when(mapper.findByIdReporter(4L)).thenReturn(expected);
        ClientsRepository repository = new ClientsRepository(mapper);

        ClientReporterView actual = repository.findClientByIdReporter(4L);

        assertSame(expected, actual);
        verify(mapper).findByIdReporter(4L);
    }

    @Test
    void findLoginClientByUsernameDelegatesToMapper() {
        ClientsMapper mapper = mock(ClientsMapper.class);
        ClientLoginView expected = new ClientLoginView(5L, "clientuser", "password1");
        when(mapper.findLoginClientByUsername("clientuser")).thenReturn(expected);
        ClientsRepository repository = new ClientsRepository(mapper);

        ClientLoginView actual = repository.findLoginClientByUsername("clientuser");

        assertSame(expected, actual);
        verify(mapper).findLoginClientByUsername("clientuser");
    }

    @Test
    void findLoginClientByIdDelegatesToMapper() {
        ClientsMapper mapper = mock(ClientsMapper.class);
        ClientLoginView expected = new ClientLoginView(5L, "loginuser", "password1");
        when(mapper.findLoginClientById(5L)).thenReturn(expected);
        ClientsRepository repository = new ClientsRepository(mapper);

        ClientLoginView actual = repository.findLoginClientById(5L);

        assertSame(expected, actual);
        verify(mapper).findLoginClientById(5L);
    }

    @Test
    void findClientsAsAdminDelegatesToMapper() {
        ClientsMapper mapper = mock(ClientsMapper.class);
        List<ClientAdminView> expected = List.of(new ClientAdminView(1L, "admin", "admin@example.com", "Admin", "User", BigDecimal.ZERO));
        when(mapper.findAllClientsAdmin()).thenReturn(expected);
        ClientsRepository repository = new ClientsRepository(mapper);

        List<ClientAdminView> actual = repository.findClientsAsAdmin();

        assertSame(expected, actual);
        verify(mapper).findAllClientsAdmin();
    }

    @Test
    void findClientsAsReporterDelegatesToMapper() {
        ClientsMapper mapper = mock(ClientsMapper.class);
        List<ClientReporterView> expected = List.of(new ClientReporterView(2L, "reporter", "reporter@example.com"));
        when(mapper.findAllClientsReporter()).thenReturn(expected);
        ClientsRepository repository = new ClientsRepository(mapper);

        List<ClientReporterView> actual = repository.findClientsAsReporter();

        assertSame(expected, actual);
        verify(mapper).findAllClientsReporter();
    }

    @Test
    void createClientPassesEntityFieldsToMapper() {
        ClientsMapper mapper = mock(ClientsMapper.class);
        Client client = client(6L, "client@example.com", "clientuser", "password1", "Client", "User", BigDecimal.ZERO);
        when(mapper.insert("client@example.com", "clientuser", "password1", "Client", "User")).thenReturn(1);
        ClientsRepository repository = new ClientsRepository(mapper);

        int actual = repository.createClient(client);

        assertEquals(1, actual);
        verify(mapper).insert("client@example.com", "clientuser", "password1", "Client", "User");
    }

    @Test
    void updateProfilePassesRequestFieldsToMapper() {
        ClientsMapper mapper = mock(ClientsMapper.class);
        ClientProfileUpdateRequest request = new ClientProfileUpdateRequest("updateduser", "Updated", "Name");
        when(mapper.updateProfile(7L, "updateduser", "Updated", "Name")).thenReturn(1);
        ClientsRepository repository = new ClientsRepository(mapper);

        int actual = repository.updateProfile(7L, request);

        assertEquals(1, actual);
        verify(mapper).updateProfile(7L, "updateduser", "Updated", "Name");
    }

    @Test
    void updatePasswordDelegatesToMapper() {
        ClientsMapper mapper = mock(ClientsMapper.class);
        when(mapper.updatePassword(8L, "newpass12")).thenReturn(1);
        ClientsRepository repository = new ClientsRepository(mapper);

        int actual = repository.updatePassword(8L, "newpass12");

        assertEquals(1, actual);
        verify(mapper).updatePassword(8L, "newpass12");
    }

    @Test
    void updateCashAmountDelegatesToMapper() {
        ClientsMapper mapper = mock(ClientsMapper.class);
        when(mapper.updateCashAmount(9L, new BigDecimal("150.00"))).thenReturn(1);
        ClientsRepository repository = new ClientsRepository(mapper);

        int actual = repository.updateCashAmount(9L, new BigDecimal("150.00"));

        assertEquals(1, actual);
        verify(mapper).updateCashAmount(9L, new BigDecimal("150.00"));
    }

    private Client client(
            Long clientId,
            String email,
            String username,
            String password,
            String firstName,
            String lastName,
            BigDecimal cashAmount) {
        Client client = new Client();
        client.setClientId(clientId);
        client.setEmail(email);
        client.setUsername(username);
        client.setPassword(password);
        client.setFirstName(firstName);
        client.setLastName(lastName);
        client.setCashAmount(cashAmount);
        return client;
    }
}