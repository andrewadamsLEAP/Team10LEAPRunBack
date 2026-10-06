package com.example.exceptions;

import com.example.DTOs.clients.LoginView;

import java.util.List;
import java.util.function.Supplier;

import com.example.DTOs.clients.ClientAdminView;
import com.example.DTOs.clients.ClientReporterView;
import com.example.entities.Client;

/**
 * Central validation utility for the trading application.
 * Provides methods for validating customer identity and order data.
 * Designed to work with JWT authentication when integrated.
 */
public class Validate {

    /**
     * Validates a customer by username and resolves to their clientId.
     * This is the primary validation method used at the API/controller layer.
     * When JWT is integrated, this will verify the authenticated user matches the username.
     *
     * @param username the customer's username (from request or JWT token)
     * @param clientLookup a function that looks up the client by username in the database
     * @return the resolved clientId to use for service layer operations
     * @throws InvalidArgumentsException if username is invalid (null, empty, etc.)
     * @throws ClientNotFoundException if the customer is not found in the database
     * 
     * Usage Example (JWT-ready):
     * <pre>
     * // Before JWT integration:
     * Long clientId = Validate.validateCustomer(
     *     "john.doe",
     *     () -> clientsService.findClientByUsername("john.doe")
     * );
     * 
     * // After JWT integration:
     * String usernameFromToken = extractFromJWT(token);
     * Long clientId = Validate.validateCustomer(
     *     usernameFromToken,
     *     () -> clientsService.findClientByUsername(usernameFromToken)
     * );
     * </pre>
     */
    public static Long validateCustomer(String username, Supplier<Client> clientLookup) {
        // Validate username format
        if (username == null || username.trim().isEmpty()) {
            throw new InvalidArgumentsException(
                "Invalid Username",
                "Username cannot be null or empty"
            );
        }

        // Look up customer in database
        Client customer = clientLookup.get();

        // Verify customer exists
        if (customer == null) {
            throw new ClientNotFoundException(
                "Customer not found: " + username
            );
        }

        return customer.getClientId();
    }

    /**
     * Validates a clientId directly (used by internal service-to-service calls).
     * Services should generally use clientId, not username, for performance.
     *
     * @param clientId the client ID to validate
     * @param clientExists a function that checks if the client exists
     * @throws InvalidArgumentsException if clientId is invalid
     * @throws ClientNotFoundException if client not found in database
     */
    public static void validateClientId(Long clientId, Supplier<Boolean> clientExists) {
        if (clientId == null || clientId <= 0) {
            throw new InvalidArgumentsException(
                "Invalid Client ID",
                "Client ID cannot be null or less than or equal to zero: " + clientId
            );
        }
        if (!clientExists.get()) {
            throw new ClientNotFoundException(clientId);
        }
    }

    /**
     * Validates a ticker symbol for order operations.
     *
     * @param ticker the ticker symbol to validate
     * @throws InvalidArgumentsException if ticker is invalid
     */
    public static void validateTicker(String ticker) {
        if (ticker == null || ticker.trim().isEmpty()) {
            throw new InvalidArgumentsException(
                "Invalid Ticker",
                "Ticker cannot be null or empty: " + ticker
            );
        }
    }

    /**
     * Validates order quantity.
     *
     * @param quantity the quantity to validate
     * @throws InvalidArgumentsException if quantity is invalid
     */
    public static void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new InvalidArgumentsException(
                "Invalid Quantity",
                "Quantity must be positive: " + quantity
            );
        }
    }

    /**
     * Validates that a customer entity exists (non-null).
     * Generic method for any Client type validation.
     *
     * @param client the client entity to validate
     * @throws ClientNotFoundException if client is null
     */
    private static void validateCustomerExists(Object client) {
        if (client == null) {
            throw new ClientNotFoundException("Customer not found");
        }
    }

    public static void validateClient(LoginView client) {
        validateCustomerExists(client);
    }

    public static void validateClient(Client client) {
        validateCustomerExists(client);
    }

    public static void validateClient(ClientAdminView client) {
        validateCustomerExists(client);
    }

    public static void validateClient(ClientReporterView client) {
        validateCustomerExists(client);
    }

    /**
     * Validates that a client list for admin view is not empty.
     *
     * @param clients the list of clients to validate
     * @throws ClientNotFoundException if list is null or empty
     */
    public static void validateClientListForAdmin(List<ClientAdminView> clients) {
        if (clients == null || clients.isEmpty()) {
            throw new ClientNotFoundException("No clients found");
        }
    }

    /**
     * Validates that a client list for reporter view is not empty.
     *
     * @param clients the list of clients to validate
     * @throws ClientNotFoundException if list is null or empty
     */
    public static void validateClientListForReporter(List<ClientReporterView> clients) {
        if (clients == null || clients.isEmpty()) {
            throw new ClientNotFoundException("No clients found");
        }
    }
}
