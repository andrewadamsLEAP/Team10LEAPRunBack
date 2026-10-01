package com.example.exceptions;

import com.example.DTOs.clients.ClientLoginView;

import java.util.List;
import java.util.function.Supplier;

import com.example.DTOs.clients.ClientAdminView;
import com.example.DTOs.clients.ClientReporterView;
import com.example.entities.Client;

public class Validate {

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

    public static void validateClient(ClientLoginView client) {
        if (client == null) {
            throw new ClientNotFoundException("Client not found");        
        }
    }

    public static void validateClient(Client client) {
        if (client == null) {
            throw new ClientNotFoundException("Client not found");        
        }
    }

    public static void validateClient(ClientAdminView client) {
        if (client == null) {
            throw new ClientNotFoundException("Client not found");        
        }
    }

    public static void validateClient(ClientReporterView client) {
        if (client == null) {
            throw new ClientNotFoundException("Client not found");        
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
