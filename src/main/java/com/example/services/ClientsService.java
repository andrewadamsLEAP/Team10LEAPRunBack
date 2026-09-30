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
import com.example.exceptions.ClientNotFoundException;
import com.example.exceptions.InvalidCredentialsException;
import com.example.exceptions.InvalidEmailException;
import com.example.exceptions.InvalidPasswordChangeException;
import com.example.exceptions.InvalidProfileUpdateException;
import com.example.exceptions.InvalidTransactionException;
import com.example.exceptions.InvalidUsernameChangeException;
import com.example.exceptions.UpdateFailedException;
import com.example.repositories.ClientsRepository;
import org.springframework.stereotype.Service;

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
            throw new InvalidEmailException("Email is already in use");
        }

        Long existingClientId = clientsRepository.findClientIdByUsername(request.getUsername());

        if (existingClientId != null) {
            throw new InvalidUsernameChangeException("Username is not unique");
        }

        int createdRows = clientsRepository.createClient(request);

        if (createdRows != 1) {
            throw new UpdateFailedException("Client signup failed");
        }

        ClientLoginView createdClient = clientsRepository.findLoginClientByUsername(request.getUsername());

        if (createdClient == null) {
            throw new UpdateFailedException("Client signup failed");
        }

        return new LoginResponse(createdClient.clientId(), createdClient.username(), null, "Signup successful");
    }

    public LoginResponse login(LoginRequest request) {
        // TODO: Encode then compare passwords when we do the whole JwT node stuff
        ClientLoginView loginClient = clientsRepository.findLoginClientByUsername(request.username());

        if (loginClient == null || !loginClient.password().equals(request.password())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        return new LoginResponse(loginClient.clientId(), loginClient.username(), null, "Login successful");
    }

    // TODO: Encode passwords and verify when we work on JwT stuff (& check if user = clientId)
    public void changePassword(Long clientId, ChangePasswordRequest request) {
        ClientLoginView client = clientsRepository.findLoginClientById(clientId);

        if (client == null) {
            throw new ClientNotFoundException("Client not found");
        }

        if (!client.password().equals(request.currentPassword())) {
            throw new InvalidPasswordChangeException("Current password is incorrect");
        }

        if (request.currentPassword().equals(request.newPassword())) {
            throw new InvalidPasswordChangeException("New password must be different from current password");
        }

        int updatedRows = clientsRepository.updatePassword(clientId, request.newPassword());

        if (updatedRows != 1) {
            throw new UpdateFailedException("Password update failed");
        }
    }

    public ClientProfileView getClientProfile(Long clientId) {
        Client client = clientsRepository.findClientById(clientId);

        if (client == null) {
            throw new ClientNotFoundException("Client not found");
        }

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

        if (client == null) {
            throw new ClientNotFoundException("Client not found");
        }

        return client;
    }

    public ClientReporterView getClientDataReporter(Long clientId) {
        ClientReporterView client = clientsRepository.findClientByIdReporter(clientId);

        if (client == null) {
            throw new ClientNotFoundException("Client not found");
        }

        return client;
    }

    public List<ClientAdminView> getAllClientDataAdmin() {
        List<ClientAdminView> clients = clientsRepository.findClientsAsAdmin();

        if (clients.isEmpty()) {
            throw new ClientNotFoundException("No clients found");
        }

        return clients;
    }

    public List<ClientReporterView> getAllClientDataReporter() {
        List<ClientReporterView> clients = clientsRepository.findClientsAsReporter();

        if (clients.isEmpty()) {
            throw new ClientNotFoundException("No clients found");
        }

        return clients;
    }

    public BigDecimal updateCashAmount(Long clientId, BigDecimal change) {
        Client client = clientsRepository.findClientById(clientId);

        if (client == null) {
            throw new ClientNotFoundException("Client not found");
        }

        BigDecimal currentBalance = client.getCashAmount();

        BigDecimal net = currentBalance.add(change);

        if (net.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidTransactionException("Invalid transaction");
        }

        int updatedRows = clientsRepository.updateCashAmount(clientId, net);

        if (updatedRows != 1) {
            throw new UpdateFailedException("Cash update failed");
        }

        return net;
    }

    public void updateProfile(Long clientId, ClientProfileUpdateRequest request) {
        Client client = clientsRepository.findClientById(clientId);

        if (client == null) {
            throw new ClientNotFoundException("Client not found");
        }

        if (!request.hasUpdates()) {
            throw new InvalidProfileUpdateException("No profile changes were provided");
        }

        if (request.username() != null) {
            Long existingClientId = clientsRepository.findClientIdByUsername(request.username());

            if (existingClientId != null && !existingClientId.equals(clientId)) {
                throw new InvalidUsernameChangeException("Username is not unique");
            }
        }
        
        int updatedRows = clientsRepository.updateProfile(clientId, request);

        if (updatedRows != 1) {
            throw new UpdateFailedException("Profile update failed");
        }
    }


}
