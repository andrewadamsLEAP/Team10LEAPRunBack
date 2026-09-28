package com.example.clients;

import org.springframework.stereotype.Repository;

import com.example.clients.DTO.ClientProfileUpdateRequest;

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

    public boolean validateLogin(String username, String password) {
        if (clientsMapper.findByUsernameAndPassword(username, password) == null) {
            return false;
        }

        return true;
    }

    public List<Client> findClients() {
        return clientsMapper.findAllClients();
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
