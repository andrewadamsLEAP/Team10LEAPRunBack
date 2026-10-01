package com.example.exceptions;

/**
 * Exception thrown when an order with a specified ID is not found.
 * Used in OrderService when an order cannot be found by its ID.
 */

public class OrderNotFound extends RuntimeException {
    public OrderNotFound(String message) {
        super(message);
    }

    public OrderNotFound(Long orderId) {
        super("Order with ID " + orderId + " not found");
    }
}