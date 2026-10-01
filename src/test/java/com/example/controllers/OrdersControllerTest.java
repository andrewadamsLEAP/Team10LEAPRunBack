package com.example.controllers;

import com.example.services.OrdersService;
import com.example.DTOs.orders.PlaceOrderRequest;
import com.example.DTOs.orders.OrderResponse;
import com.example.DTOs.orders.OrderHistoryView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdersControllerTest {

    @Mock
    private OrdersService ordersService;

    @InjectMocks
    private OrdersController ordersController;

    /**
     * Tests getting order by ID
     */
    @Test
    void getOrderById_shouldReturnOrderFromService() {
        Long orderId = 1L;
        OrderResponse mockOrder = new OrderResponse(
                orderId, "AAPL", 100, new BigDecimal("150.00"), "PENDING", null
        );

        when(ordersService.getOrderByIdAsDto(orderId)).thenReturn(mockOrder);

        OrderResponse result = ordersController.getOrderById(orderId);

        assertNotNull(result);
        assertEquals(orderId, result.orderId());
        assertEquals("AAPL", result.ticker());
        verify(ordersService, times(1)).getOrderByIdAsDto(orderId);
    }

    /**
     * Tests getting fulfilled orders for client
     */
    @Test
    void getFulfilledOrders_shouldReturnFulfilledOrdersForClient() {
        Long clientId = 1L;
        List<OrderHistoryView> mockOrders = new ArrayList<>();
        OrderHistoryView order = new OrderHistoryView(1L, "AAPL", 100, new BigDecimal("150.00"), "FULFILLED", null, null, null, null);
        mockOrders.add(order);

        when(ordersService.getFulfilledOrdersAsDto(clientId)).thenReturn(mockOrders);

        List<OrderHistoryView> result = ordersController.getFulfilledOrders(clientId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("FULFILLED", result.get(0).status());
        verify(ordersService, times(1)).getFulfilledOrdersAsDto(clientId);
    }

    /**
     * Tests getting cancelled orders
     */
    @Test
    void getCancelledOrders_shouldReturnAllCancelledOrders() {
        List<OrderHistoryView> mockOrders = new ArrayList<>();
        OrderHistoryView order = new OrderHistoryView(1L, "AAPL", 100, new BigDecimal("150.00"), "CANCELLED", null, null, null, null);
        mockOrders.add(order);

        when(ordersService.getCancelledOrdersAsDto()).thenReturn(mockOrders);

        List<OrderHistoryView> result = ordersController.getCancelledOrders();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("CANCELLED", result.get(0).status());
        verify(ordersService, times(1)).getCancelledOrdersAsDto();
    }

    /**
     * Tests getting pending sell orders for ticker
     */
    @Test
    void getPendingSellOrdersForTicker_shouldReturnSellOrdersForTicker() {
        String ticker = "AAPL";
        List<OrderHistoryView> mockOrders = new ArrayList<>();
        OrderHistoryView order = new OrderHistoryView(1L, ticker, 50, new BigDecimal("150.00"), "PENDING", null, null, null, null);
        mockOrders.add(order);

        when(ordersService.getPendingSellOrdersForTickerAsDto(ticker)).thenReturn(mockOrders);

        List<OrderHistoryView> result = ordersController.getPendingSellOrdersForTicker(ticker);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(ticker, result.get(0).ticker());
        verify(ordersService, times(1)).getPendingSellOrdersForTickerAsDto(ticker);
    }

    /**
     * Tests getting pending buy orders for ticker
     */
    @Test
    void getPendingBuyOrdersForTicker_shouldReturnBuyOrdersForTicker() {
        String ticker = "GOOGL";
        List<OrderHistoryView> mockOrders = new ArrayList<>();
        OrderHistoryView order = new OrderHistoryView(1L, ticker, 75, new BigDecimal("100.00"), "PENDING", null, null, null, null);
        mockOrders.add(order);

        when(ordersService.getPendingBuyOrdersForTickerAsDto(ticker)).thenReturn(mockOrders);

        List<OrderHistoryView> result = ordersController.getPendingBuyOrdersForTicker(ticker);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(ordersService, times(1)).getPendingBuyOrdersForTickerAsDto(ticker);
    }

    /**
     * Tests placing buy order
     */
    @Test
    void placeBuyOrder_shouldPlaceOrderAndReturnResponse() {
        PlaceOrderRequest request = new PlaceOrderRequest(1L, "AAPL", 100, new BigDecimal("150.00"));
        OrderResponse mockResponse = new OrderResponse(
                1L, "AAPL", 100, new BigDecimal("150.00"), "PENDING", null
        );

        when(ordersService.placeBuyOrderAsDto(1L, "AAPL", 100, new BigDecimal("150.00")))
                .thenReturn(mockResponse);

        OrderResponse result = ordersController.placeBuyOrder(request);

        assertNotNull(result);
        assertEquals("AAPL", result.ticker());
        verify(ordersService, times(1)).placeBuyOrderAsDto(1L, "AAPL", 100, new BigDecimal("150.00"));
    }

    /**
     * Tests placing sell order
     */
    @Test
    void placeSellOrder_shouldPlaceOrderAndReturnResponse() {
        PlaceOrderRequest request = new PlaceOrderRequest(1L, "MSFT", 50, new BigDecimal("200.00"));
        OrderResponse mockResponse = new OrderResponse(
                1L, "MSFT", 50, new BigDecimal("200.00"), "PENDING", null
        );

        when(ordersService.placeSellOrderAsDto(1L, "MSFT", 50, new BigDecimal("200.00")))
                .thenReturn(mockResponse);

        OrderResponse result = ordersController.placeSellOrder(request);

        assertNotNull(result);
        assertEquals("MSFT", result.ticker());
        verify(ordersService, times(1)).placeSellOrderAsDto(1L, "MSFT", 50, new BigDecimal("200.00"));
    }

    /**
     * Tests cancelling order
     */
    @Test
    void cancelOrder_shouldCancelOrderAndReturnResponse() {
        Long orderId = 1L;
        OrderResponse mockResponse = new OrderResponse(
                orderId, "AAPL", 100, new BigDecimal("150.00"), "CANCELLED", null
        );

        when(ordersService.cancelOrderAsDto(orderId)).thenReturn(mockResponse);

        OrderResponse result = ordersController.cancelOrder(orderId);

        assertNotNull(result);
        assertEquals("CANCELLED", result.status());
        verify(ordersService, times(1)).cancelOrderAsDto(orderId);
    }

    /**
     * Tests getting empty fulfilled orders list
     */
    @Test
    void getFulfilledOrders_shouldReturnEmptyListWhenNoOrders() {
        Long clientId = 1L;

        when(ordersService.getFulfilledOrdersAsDto(clientId)).thenReturn(new ArrayList<>());

        List<OrderHistoryView> result = ordersController.getFulfilledOrders(clientId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Tests getting cancelled orders returns empty list
     */
    @Test
    void getCancelledOrders_shouldReturnEmptyListWhenNoOrders() {
        when(ordersService.getCancelledOrdersAsDto()).thenReturn(new ArrayList<>());

        List<OrderHistoryView> result = ordersController.getCancelledOrders();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Tests multiple fulfilled orders for same client
     */
    @Test
    void getFulfilledOrders_shouldReturnMultipleOrdersForClient() {
        Long clientId = 1L;
        List<OrderHistoryView> mockOrders = new ArrayList<>();
        mockOrders.add(new OrderHistoryView(1L, "AAPL", 100, new BigDecimal("150.00"), "FULFILLED", null, null, null, null));
        mockOrders.add(new OrderHistoryView(2L, "GOOGL", 50, new BigDecimal("100.00"), "FULFILLED", null, null, null, null));

        when(ordersService.getFulfilledOrdersAsDto(clientId)).thenReturn(mockOrders);

        List<OrderHistoryView> result = ordersController.getFulfilledOrders(clientId);

        assertEquals(2, result.size());
    }
}
