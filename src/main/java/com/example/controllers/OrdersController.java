package com.example.controllers;

import com.example.services.OrdersService;
import com.example.services.ClientsService;
import com.example.DTOs.orders.PlaceOrderRequest;
import com.example.DTOs.orders.OrderResponse;
import com.example.DTOs.orders.OrderHistoryView;
import com.example.exceptions.Validate;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.util.List;

//
//  At somepoint can you or me, whoever, change the old === comments to new-style Javadoc comments
//

@RestController
@RequestMapping("/api/v1/orders")
public class OrdersController {

/**
 * The OrdersController class handles HTTP requests related to orders.
 * It provides endpoints for retrieving, creating, and updating orders.
 * Uses username-based validation (JWT-ready for future integration).
 */
private final OrdersService ordersService;
private final ClientsService clientsService;

/**
 * Constructs a new OrdersController with the specified services.
 *
 * @param ordersService the OrdersService instance to use
 * @param clientsService the ClientsService instance for customer validation
 */
public OrdersController(OrdersService ordersService, ClientsService clientsService) {
    this.ordersService = ordersService;
    this.clientsService = clientsService;
}

// =========================================================
// Get order by ID
// =========================================================

@GetMapping("/{orderId}")
public OrderResponse getOrderById(
        @PathVariable Long orderId) {
    return ordersService.getOrderByIdAsDto(orderId);
}


// =========================================================
// Get fulfilled orders for client
// =========================================================

@GetMapping("/client/{clientId}/fulfilled")
public List<OrderHistoryView> getFulfilledOrders(
        @PathVariable Long clientId) {

    return ordersService.getFulfilledOrdersAsDto(clientId);
}


// =========================================================
// Get all cancelled orders
// =========================================================

@GetMapping("/cancelled")
public List<OrderHistoryView> getCancelledOrders() {

    return ordersService.getCancelledOrdersAsDto();
}

// =========================================================
// Get cancelled orders for client
// =========================================================

@GetMapping("/client/{clientId}/cancelled")
public List<OrderHistoryView> getCancelledOrdersForClient(
        @PathVariable Long clientId) {

    return ordersService.getCancelledOrdersForClientAsDto(clientId);
}


// =========================================================
// Get all pending sell orders for ticker
// =========================================================

@GetMapping("/sell/{ticker}")
public List<OrderHistoryView> getPendingSellOrdersForTicker(
        @PathVariable String ticker) {

    return ordersService
            .getPendingSellOrdersForTickerAsDto(ticker);
}


// =========================================================
// Get all pending buy orders for ticker
// =========================================================

@GetMapping("/buy/{ticker}")
public List<OrderHistoryView> getPendingBuyOrdersForTicker(
        @PathVariable String ticker) {

    return ordersService
            .getPendingBuyOrdersForTickerAsDto(ticker);
}


/**
 * Places a buy order for the authenticated customer.
 * 
 * Currently: clientId is passed in request body.
 * With JWT: clientId will be extracted from the JWT token (validated at login time).
 * 
 * @param request the order request containing clientId, ticker, and quantity
 * @return an OrderResponse with the created order details
 * @throws com.example.exceptions.InvalidArgumentsException if validation fails
 */
@PostMapping("/buy")
public OrderResponse placeBuyOrder(
        @Valid @RequestBody PlaceOrderRequest request) {
    // For now: use clientId from request
    // When JWT lands: extract clientId from token instead of request
    return ordersService.placeBuyOrderAsDto(
            request.clientId(),
            request.ticker(),
            request.quantity()
    );
}


/**
 * Places a sell order for the authenticated customer.
 * 
 * Currently: clientId is passed in request body.
 * With JWT: clientId will be extracted from the JWT token (validated at login time).
 * 
 * @param request the order request containing clientId, ticker, and quantity
 * @return an OrderResponse with the created order details
 * @throws com.example.exceptions.InvalidArgumentsException if validation fails
 */
@PostMapping("/sell")
public OrderResponse placeSellOrder(
        @Valid @RequestBody PlaceOrderRequest request) {
    // For now: use clientId from request
    // When JWT lands: extract clientId from token instead of request
    return ordersService.placeSellOrderAsDto(
            request.clientId(),
            request.ticker(),
            request.quantity()
    );
}


// =========================================================
// Cancel an order
// =========================================================

@PostMapping("/cancel/{orderId}")
public OrderResponse cancelOrder(
        @PathVariable Long orderId) {
    return ordersService.cancelOrderAsDto(orderId);
}

}
