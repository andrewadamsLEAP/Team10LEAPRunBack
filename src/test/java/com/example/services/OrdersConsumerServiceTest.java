package com.example.services;

import com.example.entities.Order;
import com.example.exceptions.InvalidArgumentsException;
import com.example.repositories.OrdersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrdersConsumerServiceTest {

    private OrdersRepository ordersRepository;
    private HoldingsService holdingsService;
    private ClientsService clientsService;
    private OrderConsumerService orderConsumerService;

    @BeforeEach
    void setUp() {
        ordersRepository = mock(OrdersRepository.class);
        holdingsService = mock(HoldingsService.class);
        clientsService = mock(ClientsService.class);

        orderConsumerService = new OrderConsumerService(
                ordersRepository,
                holdingsService,
                clientsService
        );
    }

    @Test
    void executeOrderUpdatesPendingOrderToFulfilled() {
        Order pendingOrder = order(1L, 1L, "AAPL", Order.OrderType.BUY, 10, new BigDecimal("150.00"));
        Order fulfilledOrder = order(1L, 1L, "AAPL", Order.OrderType.BUY, 10, new BigDecimal("150.00"));
        fulfilledOrder.setOrderStatus(Order.OrderStatus.FULFILLED);

        when(ordersRepository.updateOrderStatus(1L, Order.OrderStatus.FULFILLED)).thenReturn(1);
        when(ordersRepository.getOrderById(1L)).thenReturn(fulfilledOrder);

        Order result = orderConsumerService.executeOrder(pendingOrder);

        assertNotNull(result);
        assertEquals(Order.OrderStatus.FULFILLED, result.getOrderStatus());
        verify(ordersRepository).updateOrderStatus(1L, Order.OrderStatus.FULFILLED);
        verify(holdingsService).updateHoldingsForOrder(fulfilledOrder);
        verify(clientsService).updateCashAmount(1L, new BigDecimal("150.00").multiply(BigDecimal.valueOf(10)).negate());
    }

    @Test
    void executeOrderThrowsWhenOrderIsNotPending() {
        Order cancelledOrder = order(1L, 1L, "AAPL", Order.OrderType.BUY, 10, new BigDecimal("150.00"));
        cancelledOrder.setOrderStatus(Order.OrderStatus.CANCELLED);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> orderConsumerService.executeOrder(cancelledOrder)
        );

        assertTrue(exception.getMessage().contains("Only pending orders can be executed"));
        verify(ordersRepository, never()).updateOrderStatus(anyLong(), any());
        verify(holdingsService, never()).updateHoldingsForOrder(any());
    }

    @Test
    void executeOrderThrowsWhenOrderIsNull() {
        assertThrows(
                NullPointerException.class,
                () -> orderConsumerService.executeOrder(null)
        );

        verify(ordersRepository, never()).updateOrderStatus(anyLong(), any());
        verify(holdingsService, never()).updateHoldingsForOrder(any());
    }

    @Test
    void executeOrderThrowsWhenOrderCannotBeReloaded() {
        Order pendingOrder = order(999L, 1L, "AAPL", Order.OrderType.BUY, 10, new BigDecimal("150.00"));

        when(ordersRepository.updateOrderStatus(999L, Order.OrderStatus.FULFILLED)).thenReturn(1);
        when(ordersRepository.getOrderById(999L)).thenReturn(null);

        InvalidArgumentsException exception = assertThrows(
                InvalidArgumentsException.class,
                () -> orderConsumerService.executeOrder(pendingOrder)
        );

        assertTrue(exception.getMessage().contains("Order not found: 999"));
        verify(holdingsService, never()).updateHoldingsForOrder(any());
    }

    @Test
    void executeOrderThrowsWhenUpdateFails() {
        Order pendingOrder = order(1L, 1L, "AAPL", Order.OrderType.BUY, 10, new BigDecimal("150.00"));

        when(ordersRepository.updateOrderStatus(1L, Order.OrderStatus.FULFILLED)).thenReturn(0);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> orderConsumerService.executeOrder(pendingOrder)
        );

        assertTrue(exception.getMessage().contains("is no longer pending and could not be executed"));
        verify(ordersRepository, never()).getOrderById(anyLong());
        verify(holdingsService, never()).updateHoldingsForOrder(any());
    }

    @Test
    void executeOrderThrowsWhenReloadedOrderIdIsInvalid() {
        Order pendingOrder = order(null, 1L, "AAPL", Order.OrderType.BUY, 10, new BigDecimal("150.00"));

        when(ordersRepository.updateOrderStatus(isNull(), eq(Order.OrderStatus.FULFILLED))).thenReturn(1);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orderConsumerService.executeOrder(pendingOrder)
        );

        assertEquals("Order ID must be greater than zero.", exception.getMessage());
        verify(holdingsService, never()).updateHoldingsForOrder(any());
    }

    @Test
    void executeOrderThrowsWhenHoldingsUpdateFails() {
        Order pendingOrder = order(1L, 1L, "AAPL", Order.OrderType.BUY, 10, new BigDecimal("150.00"));
        Order fulfilledOrder = order(1L, 1L, "AAPL", Order.OrderType.BUY, 10, new BigDecimal("150.00"));
        fulfilledOrder.setOrderStatus(Order.OrderStatus.FULFILLED);

        when(ordersRepository.updateOrderStatus(1L, Order.OrderStatus.FULFILLED)).thenReturn(1);
        when(ordersRepository.getOrderById(1L)).thenReturn(fulfilledOrder);
        doThrow(new RuntimeException("Holdings update failed")).when(holdingsService).updateHoldingsForOrder(fulfilledOrder);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> orderConsumerService.executeOrder(pendingOrder)
        );

        assertTrue(exception.getMessage().contains("Holdings update failed"));
    }

    private Order order(
            Long orderId,
            Long clientId,
            String ticker,
            Order.OrderType orderType,
            int quantity,
            BigDecimal price) {
        Order order = new Order();
        order.setOrderId(orderId);
        order.setClientId(clientId);
        order.setTicker(ticker);
        order.setOrderType(orderType);
        order.setOrderStatus(Order.OrderStatus.PENDING);
        order.setQuantity(quantity);
        order.setPrice(price);
        order.setOrderDate(OffsetDateTime.now());
        return order;
    }
}
