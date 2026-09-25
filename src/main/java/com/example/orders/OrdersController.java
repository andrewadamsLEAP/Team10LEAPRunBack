package com.example.orders;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
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
public Order getOrderById(
        @PathVariable Long orderId) {

    return ordersService.getOrderById(orderId);
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

@PostMapping("/buy/{clientId}/{ticker}/{quantity}/{price}")
public Order placeBuyOrder(
        @PathVariable Long clientId,
        @PathVariable String ticker,
        @PathVariable int quantity,
        @PathVariable BigDecimal price) {

    return ordersService.placeBuyOrder(
            clientId,
            ticker,
            quantity,
            price
    );
}


// =========================================================
// Place a sell order
// =========================================================

@PostMapping("/sell/{clientId}/{ticker}/{quantity}/{price}")
public Order placeSellOrder(
        @PathVariable Long clientId,
        @PathVariable String ticker,
        @PathVariable int quantity,
        @PathVariable BigDecimal price) {

    return ordersService.placeSellOrder(
            clientId,
            ticker,
            quantity,
            price
    );
}


// =========================================================
// Cancel an order
// =========================================================

@PostMapping("/cancel/{orderId}")
public Order cancelOrder(
        @PathVariable Long orderId) {

    return ordersService.cancelOrder(orderId);
}

}
