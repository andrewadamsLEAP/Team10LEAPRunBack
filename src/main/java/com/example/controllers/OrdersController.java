package com.example.controllers;

import com.example.services.OrdersService;
import com.example.authUtils.ClientIdContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import com.example.DTOs.orders.PlaceOrderRequest;
import com.example.DTOs.orders.OrderResponse;
import com.example.DTOs.orders.OrderHistoryView;

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
 */
private final OrdersService ordersService;

/**
 * Constructs a new OrdersController with the specified OrdersService.
 *
 * @param ordersService the OrdersService instance to use
 */
public OrdersController(OrdersService ordersService) {
    this.ordersService = ordersService;
}

@GetMapping("/employee/all")
@PreAuthorize("hasAnyRole('EMPLOYEE_ADMIN', 'EMPLOYEE_REPORTER')")
public List<OrderHistoryView> getAllOrdersForEmployee() {
    return ordersService.getAllOrdersAsDto();
}

@GetMapping("/employee/all/{clientId}")
@PreAuthorize("hasAnyRole('EMPLOYEE_ADMIN', 'EMPLOYEE_REPORTER')")
public List<OrderHistoryView> getAllOrdersForEmployeeByClient(
        @PathVariable Long clientId) {
    return ordersService.getAllOrdersForClientAsDto(clientId);
}

@GetMapping("/client/all/{clientId}")
@PreAuthorize("hasRole('CLIENT')")
public List<OrderHistoryView> getAllOrdersForClient(
        @PathVariable Long clientId) {
    ClientIdContextHolder.requireClient(clientId);
    return ordersService.getAllOrdersForClientAsDto(clientId);
}

// =========================================================
// Get order by ID
// =========================================================

@GetMapping("/{orderId}")
@PreAuthorize("hasRole('CLIENT')")
public OrderResponse getOrderById(
        @PathVariable Long orderId) {
    ClientIdContextHolder.requireClient(ordersService.getOrderById(orderId).getClientId());
    return ordersService.getOrderByIdAsDto(orderId);
}


// =========================================================
// Get fulfilled orders for client
// =========================================================

@GetMapping("/client/fulfilled")
@PreAuthorize("hasRole('CLIENT')")
public List<OrderHistoryView> getFulfilledOrders() {
    Long clientId = ClientIdContextHolder.requireClientId();
    return ordersService.getFulfilledOrdersAsDto(clientId);
}


// =========================================================
// Get all cancelled orders
// =========================================================

@GetMapping("/cancelled")
@PreAuthorize("hasAnyRole('EMPLOYEE_ADMIN', 'EMPLOYEE_REPORTER')")
public List<OrderHistoryView> getCancelledOrders() {

    return ordersService.getCancelledOrdersAsDto();
}

// =========================================================
// Get cancelled orders for client
// =========================================================

@GetMapping("/client/cancelled")
@PreAuthorize("hasRole('CLIENT')")
public List<OrderHistoryView> getCancelledOrdersForClient() {
    Long clientId = ClientIdContextHolder.requireClientId();
    return ordersService.getCancelledOrdersForClientAsDto(clientId);
}


// =========================================================
// Get all pending sell orders for ticker
// =========================================================

@GetMapping("/sell/{ticker}")
@PreAuthorize("hasAnyRole('EMPLOYEE_ADMIN', 'EMPLOYEE_REPORTER')")
public List<OrderHistoryView> getPendingSellOrdersForTicker(
        @PathVariable String ticker) {

    return ordersService
            .getPendingSellOrdersForTickerAsDto(ticker);
}


// =========================================================
// Get all pending buy orders for ticker
// =========================================================

@GetMapping("/buy/{ticker}")
@PreAuthorize("hasAnyRole('EMPLOYEE_ADMIN', 'EMPLOYEE_REPORTER')")
public List<OrderHistoryView> getPendingBuyOrdersForTicker(
        @PathVariable String ticker) {

    return ordersService
            .getPendingBuyOrdersForTickerAsDto(ticker);
}


// =========================================================
// Place a buy order
// =========================================================

@PostMapping("/buy")
@PreAuthorize("hasRole('CLIENT')")
public OrderResponse placeBuyOrder(
        @Valid @RequestBody PlaceOrderRequest request) {
    Long clientId = ClientIdContextHolder.requireClientId();
    return ordersService.placeBuyOrderAsDto(
        clientId,
            request.ticker(),
            request.quantity()
    );
}


// =========================================================
// Place a sell order
// =========================================================

@PostMapping("/sell")
@PreAuthorize("hasRole('CLIENT')")
public OrderResponse placeSellOrder(
        @Valid @RequestBody PlaceOrderRequest request) {
    Long clientId = ClientIdContextHolder.requireClientId();
    return ordersService.placeSellOrderAsDto(
        clientId,
            request.ticker(),
            request.quantity()
    );
}


// =========================================================
// Cancel an order
// =========================================================

@PostMapping("/cancel/{orderId}")
@PreAuthorize("hasRole('CLIENT')")
public OrderResponse cancelOrder(
        @PathVariable Long orderId) {
    ClientIdContextHolder.requireClient(ordersService.getOrderById(orderId).getClientId());
    return ordersService.cancelOrderAsDto(orderId);
}

}
