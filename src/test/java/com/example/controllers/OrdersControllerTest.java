package com.example.controllers;

import com.example.DTOs.orders.OrderHistoryView;
import com.example.DTOs.orders.OrderResponse;
import com.example.DTOs.orders.PlaceOrderRequest;
import com.example.authUtils.ClientIdContextHolder;
import com.example.entities.Order;
import com.example.services.OrdersService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdersControllerTest {
    private static final Long CLIENT_ID = 1L;

    @Mock
    private OrdersService ordersService;

    @InjectMocks
    private OrdersController ordersController;

    @BeforeEach
    void setUpContext() {
        ClientIdContextHolder.setClientId(CLIENT_ID);
    }

    @AfterEach
    void clearContext() {
        ClientIdContextHolder.clear();
    }

    @Test
    void getFulfilledOrdersUsesAuthenticatedClient() {
        List<OrderHistoryView> expected = List.of();
        when(ordersService.getFulfilledOrdersAsDto(CLIENT_ID)).thenReturn(expected);

        assertEquals(expected, ordersController.getFulfilledOrders());
        verify(ordersService).getFulfilledOrdersAsDto(CLIENT_ID);
    }

    @Test
    void placeBuyOrderUsesAuthenticatedClient() {
        PlaceOrderRequest request = new PlaceOrderRequest("AAPL", 100, new BigDecimal("150.00"));
        OrderResponse expected = new OrderResponse(1L, "AAPL", 100, new BigDecimal("150.00"), "PENDING", null);
        when(ordersService.placeBuyOrderAsDto(CLIENT_ID, "AAPL", 100)).thenReturn(expected);

        assertEquals(expected, ordersController.placeBuyOrder(request));
        verify(ordersService).placeBuyOrderAsDto(CLIENT_ID, "AAPL", 100);
    }

    @Test
    void placeSellOrderUsesAuthenticatedClient() {
        PlaceOrderRequest request = new PlaceOrderRequest("MSFT", 50, new BigDecimal("200.00"));
        OrderResponse expected = new OrderResponse(2L, "MSFT", 50, new BigDecimal("200.00"), "PENDING", null);
        when(ordersService.placeSellOrderAsDto(CLIENT_ID, "MSFT", 50)).thenReturn(expected);

        assertEquals(expected, ordersController.placeSellOrder(request));
        verify(ordersService).placeSellOrderAsDto(CLIENT_ID, "MSFT", 50);
    }

    @Test
    void getCancelledOrdersReturnsServiceResults() {
        List<OrderHistoryView> expected = List.of();
        when(ordersService.getCancelledOrdersAsDto()).thenReturn(expected);

        assertEquals(expected, ordersController.getCancelledOrders());
    }

    @Test
    void getOrderByIdRequiresAuthenticatedOrderOwner() {
        Order order = new Order();
        order.setClientId(CLIENT_ID);
        when(ordersService.getOrderById(3L)).thenReturn(order);
        OrderResponse expected = new OrderResponse(3L, "AAPL", 1, BigDecimal.TEN, "PENDING", null);
        when(ordersService.getOrderByIdAsDto(3L)).thenReturn(expected);

        assertEquals(expected, ordersController.getOrderById(3L));
        verify(ordersService).getOrderByIdAsDto(3L);
    }
}
