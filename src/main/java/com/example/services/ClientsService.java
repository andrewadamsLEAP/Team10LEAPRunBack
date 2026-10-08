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
import com.example.exceptions.ClientNotFoundException;
import com.example.exceptions.InvalidArgumentsException;
import com.example.exceptions.UpdateFailedException;
import com.example.repositories.ClientsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.exceptions.Validate;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

@Service 
public class ClientsService {
    private static final Logger logger = LoggerFactory.getLogger(ClientsService.class);
    private final ClientsRepository clientsRepository;
    // TODO: Need to integrate JwT stuff but for now we're doing it without.

    public ClientsService(ClientsRepository clientsRepository) {
        this.clientsRepository = clientsRepository;
    }


    /**
     * Handles the signup process for a new client.
     *
     * @param request the client information for signup
     * @return a LoginResponse containing the created client's details
     * @throws InvalidArgumentsException if the email or username is already in use
     * @throws UpdateFailedException if the client could not be created in the database
     */
    public LoginResponse signup(Client request) {
        logger.info("Signup attempt for email: {}", request.getEmail());
        
        Long existingEmailClientId = clientsRepository.findClientIdByEmail(request.getEmail());

        // Check if the email already exists in the database
        if (existingEmailClientId != null) {
            logger.warn("Signup failed: Email already in use - {}", request.getEmail());
            throw new InvalidArgumentsException("Invalid Email", "Email is already in use");
        }

        Long existingClientId = clientsRepository.findClientIdByUsername(request.getUsername());

        // Check if the username is already taken
        if (existingClientId != null) {
            logger.warn("Signup failed: Username not unique - {}", request.getUsername());
            throw new InvalidArgumentsException("Invalid Username", "Username is not unique");
        }

        int createdRows = clientsRepository.createClient(request);

        // If the database doesn't record that a new row is created, it means the insert failed
        if (createdRows != 1) {
            logger.error("Signup failed: Database insert returned {} rows", createdRows);
            throw new UpdateFailedException("Client signup failed");
        }

        LoginView createdClient = clientsRepository.findLoginClientByUsername(request.getUsername());

        // Check if the client was successfully created in the database
        if (createdClient == null) {
            logger.error("Signup failed: Created client not found after insert - {}", request.getUsername());
            throw new UpdateFailedException("Client signup failed");
        }

        logger.info("Signup successful for username: {}", request.getUsername());
        return new LoginResponse(createdClient.userId(), createdClient.username(), null, "Signup successful");
    }

    /**
     * Handles the login process for an existing client.
     *
     * @param request the login request containing username and password
     * @return a LoginResponse containing the client's details if login is successful
     * @throws InvalidArgumentsException if the credentials are invalid
     */
    public LoginResponse login(LoginRequest request) {
        logger.info("Login attempt for username: {}", request.username());
        
        // TODO: Encode then compare passwords when we do the whole JwT node stuff
        LoginView loginClient;
        try{
            loginClient = clientsRepository.findLoginClientByUsername(request.username());
        } catch (Exception e) {
            logger.error("Login failed: Error occurred while fetching client - {}", request.username(), e);
            throw new InvalidArgumentsException("Invalid Credentials", "Invalid username or password");
        }

        if (loginClient == null || !loginClient.password().equals(request.password())) {
            logger.warn("Login failed: Invalid credentials for username - {}", request.username());
            throw new InvalidArgumentsException("Invalid Credentials", "Invalid username or password");
        }

        logger.info("Login successful for username: {}", request.username());
        return new LoginResponse(loginClient.userId(), loginClient.username(), null, "Login successful");
    }

    /**
     * Handles the password change process for an existing client.
     *
     * @param clientId the ID of the client
     * @param request the change password request containing the current and new passwords
     * @throws ClientNotFoundException if the client does not exist
     * @throws InvalidArgumentsException if the current password is incorrect or the new password is the same as the current password
     * @throws UpdateFailedException if the password could not be updated in the database
     */
    // TODO: Encode passwords and verify when we work on JwT stuff (& check if user = clientId)
    public void changePassword(Long clientId, ChangePasswordRequest request) {
        LoginView client = clientsRepository.findLoginClientById(clientId);
        Validate.validateNotNull(client, "Client not found");

        try{
            client = clientsRepository.findLoginClientById(clientId);
            if (client == null) {
                throw new ClientNotFoundException("Client not found");
            }
        }catch (Exception e) {  
                throw new ClientNotFoundException("Client not found");  
            }  
        
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

    /**
     * Generic method to retrieve client data by view type.
     * Validates that the client exists and returns the typed view.
     *
     * @param clientId the ID of the client
     * @param fetcher a function to fetch the client by the specific view
     * @return the client object in the requested view format
     * @throws ClientNotFoundException if the client does not exist
     * @param <T> the type of client view (ClientProfileView, ClientAdminView, ClientReporterView)
     */
    private <T> T getClientByView(Long clientId, java.util.function.Function<ClientsRepository, T> fetcher) {
        T client = fetcher.apply(clientsRepository);
        Validate.validateNotNull(client, "Client not found");
        return client;
    }

    /**
     * Retrieves the profile information for a specific client.
     *
     * @param clientId the ID of the client
     * @return a ClientProfileView containing the client's profile information
     * @throws ClientNotFoundException if the client does not exist
     */
    public ClientProfileView getClientProfile(Long clientId) {
        Client client;
        try{
            client = clientsRepository.findClientById(clientId);
            if (client == null) {
                throw new ClientNotFoundException("Client not found");
            }
        }catch (Exception e) {  
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

    /**
     * Retrieves the client data for a specific client as an admin view.
     *
     * @param clientId the ID of the client
     * @return a ClientAdminView containing the client's data for admin purposes
     * @throws ClientNotFoundException if the client does not exist
     */
    public ClientAdminView getClientDataAdmin(Long clientId) {
        return getClientByView(clientId, repo -> repo.findClientByIdAdmin(clientId));
    }

    /**
     * Retrieves the client data for a specific client as a reporter view.
     *
     * @param clientId the ID of the client
     * @return a ClientReporterView containing the client's data for reporter purposes
     * @throws ClientNotFoundException if the client does not exist
     */
    public ClientReporterView getClientDataReporter(Long clientId) {
        return getClientByView(clientId, repo -> repo.findClientByIdReporter(clientId));
    }

    /**
     * Retrieves all client data for admin view.
     *
     * @return a list of ClientAdminView containing all clients' data for admin purposes
     * @throws ClientNotFoundException if no clients are found
     */
    public List<ClientAdminView> getAllClientDataAdmin() {
        List<ClientAdminView> clients;
        try{
            clients = clientsRepository.findClientsAsAdmin();
            if (clients == null || clients.isEmpty()) {
                throw new ClientNotFoundException("Clients not found");
            }
        }catch (Exception e) {  
            throw new ClientNotFoundException("Clients not found");  
        }  

        return clients;
    }

    /**
     * Retrieves all client data for reporter view.
     *
     * @return a list of ClientReporterView containing all clients' data for reporter purposes
     * @throws ClientNotFoundException if no clients are found
     */ 
    public List<ClientReporterView> getAllClientDataReporter() {
        List<ClientReporterView> clients = clientsRepository.findClientsAsReporter();
        Validate.validateListNotEmpty(clients, "No clients found");
        return clients;
    }


    /**
     * Main method for updating cash amount which is within the clients table
     *
     * @param clientId the ID of the client
     * @param change the amount to change the client's cash balance by
     * @return the new cash balance
     * @throws InvalidArgumentsException if the transaction is invalid
     * @throws UpdateFailedException if the update fails
     */
    public BigDecimal updateCashAmount(Long clientId, BigDecimal change) {
        Client client = clientsRepository.findClientById(clientId);
        Validate.validateNotNull(client, "Client not found");

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

    /**
     * Main method to update the client profile
     *
     * @param clientId the ID of the client
     * @param request the profile update request containing the new profile information
     * @throws InvalidArgumentsException if the profile update is invalid
     * @throws UpdateFailedException if the update fails
     */
    public void updateProfile(Long clientId, ClientProfileUpdateRequest request) {
        Client client;
        try{
            client = clientsRepository.findClientById(clientId);
            if (client == null) {
                throw new ClientNotFoundException("Client not found");
            }
        }catch (Exception e) {
            throw new ClientNotFoundException("Client not found");
        }

        Validate.validateNotNull(client, "Client not found");

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