package com.example.exceptions;

import com.example.DTOs.clients.LoginView;

import java.util.List;
import java.util.function.Supplier;

import com.example.DTOs.clients.ClientAdminView;
import com.example.DTOs.clients.ClientReporterView;
import com.example.DTOs.clients.LoginView;
import com.example.entities.Client;
import com.example.repositories.ClientsRepository;
import com.example.services.ClientsService;

public class Validate {
    private final ClientsRepository clientsRepository;

    public Validate(ClientsRepository clientsRepository)
    {
        this.clientsRepository = clientsRepository;
    }

    public static void validateClientId(Long clientId, Supplier<Boolean> clientExists) {
        if (clientId == null || clientId <= 0) {
            throw new InvalidArgumentsException("Invalid Client ID","Client ID cannot be null or less than or equal to zero: " + clientId);
        }
        if (!clientExists.get()) {
            throw new ClientNotFoundException(clientId);
        }
    }

    public static void validateTicker(String ticker) {
        if (ticker == null || ticker.trim().isEmpty()) {
            throw new InvalidArgumentsException("Invalid Ticker","Ticker cannot be null or empty: " + ticker);
        }
    }
    
    public static void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new InvalidArgumentsException("Invalid Quantity","Quantity must be positive: " + quantity);
        }

    }

    public static void validateClient(LoginView client) {
        if (client == null) {
            throw new ClientNotFoundException("Client not found");        
        }
    }

   public void validateClient(Long clientId, String function) {
        if (function == "changePassword") {
            try {
            LoginView clientLoginView = clientsRepository.findLoginClientById(clientId);
            }
            catch (Exception e) {  
                throw new ClientNotFoundException("Client not found: ", e);  
            }      
        }
        else if (function == "getClientProfile") {
            Client client = clientsRepository.findClientById(clientId);
            if (client == null) {
                throw new ClientNotFoundException("Client not found");        
            }
        }
        else if(function == "getClientDataAdmin") {
            ClientAdminView clientAdminView = clientsRepository.findAdminClientById(clientId);
            if (clientAdminView == null) {
                throw new ClientNotFoundException("Client not found");        
            }
        }
        else if(function == "getClientDataReporter") {
            ClientReporterView clientReporterView = clientsRepository.findReporterClientById(clientId);
            if (clientReporterView == null) {
                throw new ClientNotFoundException("Client not found");        
            }
        }
        else if(function == "getAllClientDataAdmin"){
            List<ClientAdminView> clientsListForAdmin = clientsRepository.findClientsAsAdmin();
            if (clientsListForAdmin == null || clientsListForAdmin.isEmpty()) {
                throw new ClientNotFoundException("No clients found");
            }
        }
        else if(function == "getAllClientDataReporter"){
            List<ClientReporterView> clientsListForReporter = clientsRepository.findClientsAsReporter();
            if (clientsListForReporter == null || clientsListForReporter.isEmpty()) {
                throw new ClientNotFoundException("No clients found");
            }
        }
    }

    

    public static void validateClientListForAdmin(List<ClientAdminView> clients) {
        if (clients == null || clients.isEmpty()) {
            throw new ClientNotFoundException("No clients found");
        }
    }

    public static void validateClientListForReporter(List<ClientReporterView> clients) {
        if (clients == null || clients.isEmpty()) {
            throw new ClientNotFoundException("No clients found");
        }
    
}
}