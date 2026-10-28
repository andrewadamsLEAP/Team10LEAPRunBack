package com.example.services;

import org.springframework.stereotype.Service;

import com.example.DTOs.clients.ClientAdminView;
import com.example.DTOs.clients.AdminClientProfileUpdateRequest;
import com.example.DTOs.clients.ClientProfileUpdateRequest;
import com.example.DTOs.clients.ClientProfileView;
import com.example.DTOs.clients.ClientReporterView;
import com.example.entities.Client;
import com.example.repositories.ClientsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.exceptions.ClientNotFoundException;
import com.example.exceptions.InvalidArgumentsException;
import com.example.exceptions.UpdateFailedException;
import com.example.exceptions.Validate;

import java.math.BigDecimal;
import java.util.List;

@Service 
public class ClientsService {
    private static final Logger logger = LoggerFactory.getLogger(ClientsService.class);
    private final ClientsRepository clientsRepository;

    public ClientsService(ClientsRepository clientsRepository) {
        this.clientsRepository = clientsRepository;
    }


    /**
     * Retrieves the profile information for a specific client.
     *
     * @param clientId the ID of the client
     * @return a ClientProfileView containing the client's profile information
     * @throws IllegalArgumentException if the client does not exist wtithin validate client
     */
    public ClientProfileView getClientProfile(Long clientId) {
        Client client = Validate.validateUser(clientsRepository.findClientById(clientId), "client");

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
     * @throws IllegalArgumentException if the client does not exist within validate client
     */
    public ClientAdminView getClientDataAdmin(Long clientId) {
        
        ClientAdminView client = Validate.validateUser(clientsRepository.findClientByIdAdmin(clientId), "client");

        return client;
    }




    /**
     * Retrieves the client data for a specific client as a reporter view.
     *
     * @param clientId the ID of the client
     * @return a ClientReporterView containing the client's data for reporter purposes
     * @throws IllegalArgumentException if the client does not exist within validate client
     */
    public ClientReporterView getClientDataReporter(Long clientId) {

        ClientReporterView client = Validate.validateUser(clientsRepository.findClientByIdReporter(clientId), "client");

        return client;
    }




    /**
     * Retrieves all client data for admin view.
     *
     * @return a list of ClientAdminView containing all clients' data for admin purposes
     * @throws IllegalArgumentException if the client list is invalid within validate client list for admin
     */
    public List<ClientAdminView> getAllClientDataAdmin() {
        List<ClientAdminView> clients;
        try{
            clients = clientsRepository.findClientsAsAdmin();
            if (clients == null || clients.isEmpty()) {
                throw new ClientNotFoundException("No clients found");
            }
        }catch (Exception e) {  
            throw new ClientNotFoundException("No clients found");  
        }  

        return clients;
    }




    /**
     * Retrieves all client data for reporter view.
     *
     * @return a list of ClientReporterView containing all clients' data for reporter purposes
     * @throws IllegalArgumentException if the client list is invalid within validate client list for reporter
     */ 
    public List<ClientReporterView> getAllClientDataReporter() {
        List<ClientReporterView> clients;
        try{
            clients = clientsRepository.findClientsAsReporter();
            if (clients == null || clients.isEmpty()) {
                throw new ClientNotFoundException("No clients found");
            }
        }catch (Exception e) {  
            throw new ClientNotFoundException("No clients found");  
        }  

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
        Client client = Validate.validateUser(clientsRepository.findClientById(clientId), "client");

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
        updateProfile(clientId, request.username(), request.firstName(), request.lastName(), null,
                request.hasUpdates());
    }

    public void updateClientProfileAsAdmin(Long clientId, AdminClientProfileUpdateRequest request) {
        updateProfile(clientId, request.username(), request.firstName(), request.lastName(), request.email(),
                request.hasUpdates());
    }

    private void updateProfile(Long clientId, String username, String firstName, String lastName,
            String email, boolean hasUpdates) {
        Client client = Validate.validateUser(clientsRepository.findClientById(clientId), "client");
    

        if (!hasUpdates) {
            throw new InvalidArgumentsException("Invalid Profile Update", "No profile changes were provided");
        }

        if (username != null && !username.isBlank()) {
            Long existingClientId = clientsRepository.findClientIdByUsername(username);

            if (existingClientId != null && !existingClientId.equals(clientId)) {
                throw new InvalidArgumentsException("Invalid Username Change", "Username is not unique");
            }
        }

        if (email != null && !email.isBlank()) {
            Long existingClientId = clientsRepository.findClientIdByEmail(email);

            if (existingClientId != null && !existingClientId.equals(clientId)) {
                throw new InvalidArgumentsException("Invalid Email Change", "Email is not unique");
            }
        }
        
        int updatedRows = clientsRepository.updateProfile(clientId, username, firstName, lastName, email);

        if (updatedRows != 1) {
            throw new UpdateFailedException("Profile update failed");
        }
    }


}