package com.example.exceptions;

import java.util.List;
import java.util.function.Supplier;

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

    /**
     * Generic null validation for any object type.
     * @param obj the object to validate
     * @param message the error message if null
     * @param <T> the type of object
     * @throws ClientNotFoundException if object is null
     */
    public static <T> void validateNotNull(T obj, String message) {
        if (obj == null) {
            throw new ClientNotFoundException(message);
        }
    }

    /**
     * Generic null or empty validation for lists.
     * @param list the list to validate
     * @param message the error message if null or empty
     * @param <T> the type of list elements
     * @throws ClientNotFoundException if list is null or empty
     */
    public static <T> void validateListNotEmpty(List<T> list, String message) {
        if (list == null || list.isEmpty()) {
            throw new ClientNotFoundException(message);
        }
    }
}