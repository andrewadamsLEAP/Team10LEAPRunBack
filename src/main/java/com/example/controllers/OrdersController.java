package com.example.controllers;

import com.example.entities.Order;
import com.example.services.OrdersService;
import com.example.dtos.orders.PlaceOrderRequest;
import com.example.dtos.orders.OrderResponse;
import com.example.dtos.orders.OrderHistoryView;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrdersController {


private final OrdersService ordersService;

public OrdersController(OrdersService ordersService) {
    this.ordersService = ordersService;
}

// =========================================================
// Get order by ID
// =========================================================

@GetMapping("/{orderId}")
public OrderResponse getOrderById(
        @PathVariable Long orderId) {
    Order order = ordersService.getOrderById(orderId);
    return convertToOrderResponse(order);
}


// =========================================================
// Get fulfilled orders for client
// =========================================================

@GetMapping("/client/{clientId}/fulfilled")
public List<Order> getFulfilledOrders(
        @PathVariable Long clientId) {

    return ordersService.getFulfilledOrders(clientId);
}


// =========================================================
// Get all cancelled orders
// =========================================================

@GetMapping("/cancelled")
public List<Order> getCancelledOrders() {

    return ordersService.getCancelledOrders();
}


// =========================================================
// Get all pending sell orders for ticker
// =========================================================

@GetMapping("/sell/{ticker}")
public List<Order> getPendingSellOrdersForTicker(
        @PathVariable String ticker) {

    return ordersService
            .getPendingSellOrdersForTicker(ticker);
}


// =========================================================
// Get all pending buy orders for ticker
// =========================================================

@GetMapping("/buy/{ticker}")
public List<Order> getPendingBuyOrdersForTicker(
        @PathVariable String ticker) {

    return ordersService
            .getPendingBuyOrdersForTicker(ticker);
}


// =========================================================
// Place a buy order
// =========================================================

@PostMapping("/buy")
public OrderResponse placeBuyOrder(
        @Valid @RequestBody PlaceOrderRequest request) {
    Order order = ordersService.placeBuyOrder(
            request.clientId(),
            request.ticker(),
            request.quantity(),
            request.price()
    );
    return convertToOrderResponse(order);
}


// =========================================================
// Place a sell order
// =========================================================

@PostMapping("/sell")
public OrderResponse placeSellOrder(
        @Valid @RequestBody PlaceOrderRequest request) {
    Order order = ordersService.placeSellOrder(
            request.clientId(),
            request.ticker(),
            request.quantity(),
            request.price()
    );
    return convertToOrderResponse(order);
}


// =========================================================
// Cancel an order
// =========================================================

@PostMapping("/cancel/{orderId}")
public OrderResponse cancelOrder(
        @PathVariable Long orderId) {
    Order order = ordersService.cancelOrder(orderId);
    return convertToOrderResponse(order);
}

// =========================================================
// Helper: Convert Order entity to OrderResponse DTO
// =========================================================
private OrderResponse convertToOrderResponse(Order order) {
    return new OrderResponse(
            order.getOrderId(),
            order.getTicker(),
            order.getQuantity(),
            order.getPrice(),
            order.getOrderStatus().name(),
            order.getOrderDate().toLocalDateTime()
    );
}

}
