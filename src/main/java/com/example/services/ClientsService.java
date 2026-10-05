package com.example.services;

import com.example.DTOs.clients.ChangePasswordRequest;
import com.example.DTOs.clients.ClientAdminView;
import com.example.DTOs.clients.LoginView;
import com.example.DTOs.clients.ClientProfileUpdateRequest;
import com.example.DTOs.clients.ClientProfileView;
import com.example.DTOs.clients.ClientReporterView;
import com.example.DTOs.clients.LoginRequest;
import com.example.DTOs.clients.LoginResponse;
import com.example.entities.Client;
import com.example.exceptions.InvalidArgumentsException;
import com.example.exceptions.UpdateFailedException;
import com.example.repositories.ClientsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.exceptions.Validate;

import java.math.BigDecimal;
import java.util.List;

@Service 
public class ClientsService {
    private static final Logger logger = LoggerFactory.getLogger(ClientsService.class);
    private final ClientsRepository clientsRepository;
    // TODO: Need to integrate JwT stuff but for now we're doing it without.

    public ClientsService(ClientsRepository clientsRepository) {
        this.clientsRepository = clientsRepository;
    }

    public LoginResponse signup(Client request) {
        logger.info("Signup attempt for email: {}", request.getEmail());
        
        Long existingEmailClientId = clientsRepository.findClientIdByEmail(request.getEmail());

        if (existingEmailClientId != null) {
            logger.warn("Signup failed: Email already in use - {}", request.getEmail());
            throw new InvalidArgumentsException("Invalid Email", "Email is already in use");
        }

        Long existingClientId = clientsRepository.findClientIdByUsername(request.getUsername());

        if (existingClientId != null) {
            logger.warn("Signup failed: Username not unique - {}", request.getUsername());
            throw new InvalidArgumentsException("Invalid Username", "Username is not unique");
        }

        int createdRows = clientsRepository.createClient(request);

        if (createdRows != 1) {
            logger.error("Signup failed: Database insert returned {} rows", createdRows);
            throw new UpdateFailedException("Client signup failed");
        }

        LoginView createdClient = clientsRepository.findLoginClientByUsername(request.getUsername());

        if (createdClient == null) {
            logger.error("Signup failed: Created client not found after insert - {}", request.getUsername());
            throw new UpdateFailedException("Client signup failed");
        }

        logger.info("Signup successful for username: {}", request.getUsername());
        return new LoginResponse(createdClient.userId(), createdClient.username(), null, "Signup successful");
    }

    public LoginResponse login(LoginRequest request) {
        logger.info("Login attempt for username: {}", request.username());
        
        // TODO: Encode then compare passwords when we do the whole JwT node stuff
        LoginView loginClient = clientsRepository.findLoginClientByUsername(request.username());

        if (loginClient == null || !loginClient.password().equals(request.password())) {
            logger.warn("Login failed: Invalid credentials for username - {}", request.username());
            throw new InvalidArgumentsException("Invalid Credentials", "Invalid username or password");
        }

        logger.info("Login successful for username: {}", request.username());
        return new LoginResponse(loginClient.userId(), loginClient.username(), null, "Login successful");
    }

    // TODO: Encode passwords and verify when we work on JwT stuff (& check if user = clientId)
    public void changePassword(Long clientId, ChangePasswordRequest request) {
        LoginView client = clientsRepository.findLoginClientById(clientId);

        Validate.validateClient(client);

        if (!client.password().equals(request.currentPassword())) {
            throw new InvalidArgumentsException("Invalid Password Change", "Current password is incorrect");
        }

        if (request.currentPassword().equals(request.newPassword())) {
            throw new InvalidArgumentsException("Invalid Password Change", "New password must be different from current password");
        }

        int updatedRows = clientsRepository.updatePassword(clientId, request.newPassword());

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

        if (request.username() != null) {
            Long existingClientId = clientsRepository.findClientIdByUsername(request.username());

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