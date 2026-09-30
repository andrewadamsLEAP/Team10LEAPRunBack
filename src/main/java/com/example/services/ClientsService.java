package com.example.services;

import com.example.dtos.clients.ChangePasswordRequest;
import com.example.dtos.clients.ClientAdminView;
import com.example.dtos.clients.ClientLoginView;
import com.example.dtos.clients.ClientProfileUpdateRequest;
import com.example.dtos.clients.ClientProfileView;
import com.example.dtos.clients.ClientReporterView;
import com.example.dtos.clients.LoginRequest;
import com.example.dtos.clients.LoginResponse;
import com.example.entities.Client;
import com.example.exceptions.InvalidArgumentsException;
import com.example.exceptions.UpdateFailedException;
import com.example.repositories.ClientsRepository;
import org.springframework.stereotype.Service;

import com.example.exceptions.Validate;

import java.math.BigDecimal;
import java.util.List;

@Service 
public class ClientsService {
    private final ClientsRepository clientsRepository;
    // TODO: Need to integrate JwT stuff but for now we're doing it without.

    public ClientsService(ClientsRepository clientsRepository) {
        this.clientsRepository = clientsRepository;
    }

    public LoginResponse signup(Client request) {
        Long existingEmailClientId = clientsRepository.findClientIdByEmail(request.getEmail());

        if (existingEmailClientId != null) {
            throw new InvalidArgumentsException("Invalid Email", "Email is already in use");
        }

        Long existingClientId = clientsRepository.findClientIdByUsername(request.getUsername());

        if (existingClientId != null) {
            throw new InvalidArgumentsException("Invalid Username", "Username is not unique");
        }

        int createdRows = clientsRepository.createClient(request);

        if (createdRows != 1) {
            throw new UpdateFailedException("Client signup failed");
        }

        ClientLoginView createdClient = clientsRepository.findLoginClientByUsername(request.getUsername());

        if (createdClient == null) {
            throw new UpdateFailedException("Client signup failed");
        }

        return new LoginResponse(createdClient.getClientId(), createdClient.getUsername(), null, "Signup successful");
    }

    public LoginResponse login(LoginRequest request) {
        // TODO: Encode then compare passwords when we do the whole JwT node stuff
        ClientLoginView loginClient = clientsRepository.findLoginClientByUsername(request.getUsername());

        if (loginClient == null || !loginClient.getPassword().equals(request.getPassword())) {
            throw new InvalidArgumentsException("Invalid Credentials", "Invalid username or password");
        }

        return new LoginResponse(loginClient.getClientId(), loginClient.getUsername(), null, "Login successful");
    }

    // TODO: Encode passwords and verify when we work on JwT stuff (& check if user = clientId)
    public void changePassword(Long clientId, ChangePasswordRequest request) {
        ClientLoginView client = clientsRepository.findLoginClientById(clientId);

        Validate.validateClient(client);

        if (!client.getPassword().equals(request.getCurrentPassword())) {
            throw new InvalidArgumentsException("Invalid Password Change", "Current password is incorrect");
        }

        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new InvalidArgumentsException("Invalid Password Change", "New password must be different from current password");
        }

        int updatedRows = clientsRepository.updatePassword(clientId, request.getNewPassword());

        if (updatedRows != 1) {
            throw new UpdateFailedException("Password update failed");
        }
    }

    public ClientProfileView getClientProfile(Long clientId) {
        Client client = clientsRepository.findClientById(clientId);

        Validate.validateClient(client);

        return new ClientProfileView(
                client.getClientId(),
                client.getEmail(),
                client.getUsername(),
                client.getFirstName(),
                client.getLastName(),
                client.getCashAmount());
    }

    public ClientAdminView getClientDataAdmin(Long clientId) {
        ClientAdminView client = clientsRepository.findClientByIdAdmin(clientId);

        Validate.validateClient(client);

        return client;
    }

    public ClientReporterView getClientDataReporter(Long clientId) {
        ClientReporterView client = clientsRepository.findClientByIdReporter(clientId);

        Validate.validateClient(client);

        return client;
    }

    public List<ClientAdminView> getAllClientDataAdmin() {
        List<ClientAdminView> clients = clientsRepository.findClientsAsAdmin();

        Validate.validateClientListForAdmin(clients);

        return clients;
    }

    public List<ClientReporterView> getAllClientDataReporter() {
        List<ClientReporterView> clients = clientsRepository.findClientsAsReporter();

        Validate.validateClientListForReporter(clients);

        return clients;
    }

    public BigDecimal updateCashAmount(Long clientId, BigDecimal change) {
        Client client = clientsRepository.findClientById(clientId);

        Validate.validateClient(client);

        BigDecimal currentBalance = client.getCashAmount();

        BigDecimal net = currentBalance.add(change);

        if (net.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidArgumentsException("Invalid Transaction", "Invalid transaction");
        }

        int updatedRows = clientsRepository.updateCashAmount(clientId, net);

        if (updatedRows != 1) {
            throw new UpdateFailedException("Cash update failed");
        }

        return net;
    }

    public void updateProfile(Long clientId, ClientProfileUpdateRequest request) {
        Client client = clientsRepository.findClientById(clientId);

        Validate.validateClient(client);

        if (!request.hasUpdates()) {
            throw new InvalidArgumentsException("Invalid Profile Update", "No profile changes were provided");
        }

        if (request.getUsername() != null) {
            Long existingClientId = clientsRepository.findClientIdByUsername(request.getUsername());

            if (existingClientId != null && !existingClientId.equals(clientId)) {
                throw new InvalidArgumentsException("Invalid Username Change", "Username is not unique");
            }
        }
        
        int updatedRows = clientsRepository.updateProfile(clientId, request);

        if (updatedRows != 1) {
            throw new UpdateFailedException("Profile update failed");
        }
    }


}
