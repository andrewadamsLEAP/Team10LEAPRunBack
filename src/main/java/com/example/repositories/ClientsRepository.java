package com.example.repositories;

import com.example.DTOs.clients.ClientAdminView;
import com.example.DTOs.clients.ClientLoginView;
import com.example.mappers.ClientsMapper;
import com.example.DTOs.clients.ClientProfileUpdateRequest;
import com.example.DTOs.clients.ClientReporterView;
import com.example.entities.Client;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository 
public class ClientsRepository {
    private final ClientsMapper clientsMapper;

    public ClientsRepository(ClientsMapper clientsMapper) {
        this.clientsMapper = clientsMapper;
    }

    public Client findClientById(Long id) {
        return clientsMapper.findById(id);
    }

    public Long findClientIdByEmail(String email) {
        return clientsMapper.findClientIdByEmail(email);
    }

    public Long findClientIdByUsername(String username) {
        return clientsMapper.findClientIdByUsername(username);
    }

    public ClientAdminView findClientByIdAdmin(Long clientId) {
        return clientsMapper.findByIdAdmin(clientId);
    }

    public ClientReporterView findClientByIdReporter(Long clientId) {
        return clientsMapper.findByIdReporter(clientId);
    }

    public ClientLoginView findLoginClientByUsername(String username) {
        return clientsMapper.findLoginClientByUsername(username);
    }

    public ClientLoginView findLoginClientById(Long clientId) {
        return clientsMapper.findLoginClientById(clientId);
    }

    public List<ClientAdminView> findClientsAsAdmin() {
        return clientsMapper.findAllClientsAdmin();
    }

    public List<ClientReporterView> findClientsAsReporter() {
        return clientsMapper.findAllClientsReporter();
    }

    public int createClient(Client client) {
        return clientsMapper.insert(
            client.getEmail(),
            client.getUsername(),
            client.getPassword(),
            client.getFirstName(),
            client.getLastName()
        );
    }

     public int updateProfile(Long clientId, ClientProfileUpdateRequest request) {
        return clientsMapper.updateProfile(
            clientId,
            request.getUsername(),
            request.getFirstName(),
            request.getLastName()
        );
    }

    public int updatePassword(Long clientId, String hashedPassword) {
        return clientsMapper.updatePassword(clientId, hashedPassword);
    }

    public int updateCashAmount(Long clientId, BigDecimal cashAmount) {
        return clientsMapper.updateCashAmount(clientId, cashAmount);
    }

    
}
