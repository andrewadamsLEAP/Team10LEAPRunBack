package com.example.repositories;

import com.example.entities.Order;
import com.example.mappers.OrdersMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdersRepositoryTest {

    @Mock
    private OrdersMapper ordersMapper;

    @InjectMocks
    private OrdersRepository ordersRepository;

    /**
     * Tests getting order by ID
     */
    @Test
    void getOrderById_shouldReturnOrderFromMapper() {
        Long orderId = 1L;
        Order mockOrder = new Order();
        mockOrder.setOrderId(orderId);
        mockOrder.setTicker("AAPL");

        when(ordersMapper.getOrderById(orderId)).thenReturn(mockOrder);

        Order result = ordersRepository.getOrderById(orderId);

        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals("AAPL", result.getTicker());
        verify(ordersMapper, times(1)).getOrderById(orderId);
    }

    /**
     * Tests getting fulfilled orders for client
     */
    @Test
    void getFulfilledOrders_shouldReturnListOfFulfilledOrders() {
        Long clientId = 1L;
        List<Order> mockOrders = new ArrayList<>();
        
        Order order = new Order();
        order.setOrderId(1L);
        order.setClientId(clientId);
        order.setOrderStatus(Order.OrderStatus.FULFILLED);
        mockOrders.add(order);

        when(ordersMapper.getFulfilledOrders(clientId)).thenReturn(mockOrders);

        List<Order> result = ordersRepository.getFulfilledOrders(clientId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Order.OrderStatus.FULFILLED, result.get(0).getOrderStatus());
        verify(ordersMapper, times(1)).getFulfilledOrders(clientId);
    }

    /**
     * Tests getting cancelled orders
     */
    @Test
    void getCancelledOrders_shouldReturnListOfCancelledOrders() {
        List<Order> mockOrders = new ArrayList<>();
        
        Order order = new Order();
        order.setOrderId(1L);
        order.setOrderStatus(Order.OrderStatus.CANCELLED);
        mockOrders.add(order);

        when(ordersMapper.getCancelledOrders()).thenReturn(mockOrders);

        List<Order> result = ordersRepository.getCancelledOrders();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Order.OrderStatus.CANCELLED, result.get(0).getOrderStatus());
        verify(ordersMapper, times(1)).getCancelledOrders();
    }

    /**
     * Tests getting pending sell orders for ticker
     */
    @Test
    void getPendingSellOrdersForTicker_shouldReturnSellOrdersForTicker() {
        String ticker = "AAPL";
        List<Order> mockOrders = new ArrayList<>();
        
        Order order = new Order();
        order.setOrderId(1L);
        order.setTicker(ticker);
        order.setOrderType(Order.OrderType.SELL);
        order.setOrderStatus(Order.OrderStatus.PENDING);
        mockOrders.add(order);

        when(ordersMapper.getPendingSellOrdersForTicker(ticker)).thenReturn(mockOrders);

        List<Order> result = ordersRepository.getPendingSellOrdersForTicker(ticker);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(ticker, result.get(0).getTicker());
        assertEquals(Order.OrderType.SELL, result.get(0).getOrderType());
        verify(ordersMapper, times(1)).getPendingSellOrdersForTicker(ticker);
    }

    /**
     * Tests getting pending buy orders for ticker
     */
    @Test
    void getPendingBuyOrdersForTicker_shouldReturnBuyOrdersForTicker() {
        String ticker = "GOOGL";
        List<Order> mockOrders = new ArrayList<>();
        
        Order order = new Order();
        order.setOrderId(1L);
        order.setTicker(ticker);
        order.setOrderType(Order.OrderType.BUY);
        order.setOrderStatus(Order.OrderStatus.PENDING);
        mockOrders.add(order);

        when(ordersMapper.getPendingBuyOrdersForTicker(ticker)).thenReturn(mockOrders);

        List<Order> result = ordersRepository.getPendingBuyOrdersForTicker(ticker);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(ticker, result.get(0).getTicker());
        assertEquals(Order.OrderType.BUY, result.get(0).getOrderType());
        verify(ordersMapper, times(1)).getPendingBuyOrdersForTicker(ticker);
    }

    /**
     * Tests creating order
     */
    @Test
    void createOrder_shouldReturnCreatedOrder() {
        Order orderToCreate = new Order();
        orderToCreate.setClientId(1L);
        orderToCreate.setTicker("AAPL");
        orderToCreate.setQuantity(100);

        doNothing().when(ordersMapper).createOrder(any(Order.class));

        Order result = ordersRepository.createOrder(orderToCreate);

        assertNotNull(result);
        assertEquals(1L, result.getClientId());
        assertEquals("AAPL", result.getTicker());
        verify(ordersMapper, times(1)).createOrder(any(Order.class));
    }

    /**
     * Tests updating order status
     */
    @Test
    void updateOrderStatus_shouldUpdateAndReturnAffectedRows() {
        Long orderId = 1L;
        Order.OrderStatus newStatus = Order.OrderStatus.FULFILLED;

        when(ordersMapper.updateOrderStatus(orderId, newStatus)).thenReturn(1);

        int result = ordersRepository.updateOrderStatus(orderId, newStatus);

        assertEquals(1, result);
        verify(ordersMapper, times(1)).updateOrderStatus(orderId, newStatus);
    }

    /**
     * Tests getting fulfilled orders returns empty list
     */
    @Test
    void getFulfilledOrders_shouldReturnEmptyListWhenNoOrders() {
        Long clientId = 1L;

        when(ordersMapper.getFulfilledOrders(clientId)).thenReturn(new ArrayList<>());

        List<Order> result = ordersRepository.getFulfilledOrders(clientId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Tests getting cancelled orders returns empty list
     */
    @Test
    void getCancelledOrders_shouldReturnEmptyListWhenNoOrders() {
        when(ordersMapper.getCancelledOrders()).thenReturn(new ArrayList<>());

        List<Order> result = ordersRepository.getCancelledOrders();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Tests getting multiple orders for ticker
     */
    @Test
    void getPendingSellOrdersForTicker_shouldReturnMultipleOrdersForTicker() {
        String ticker = "AAPL";
        List<Order> mockOrders = new ArrayList<>();
        
        Order order1 = new Order();
        order1.setOrderId(1L);
        order1.setTicker(ticker);
        mockOrders.add(order1);

        Order order2 = new Order();
        order2.setOrderId(2L);
        order2.setTicker(ticker);
        mockOrders.add(order2);

        when(ordersMapper.getPendingSellOrdersForTicker(ticker)).thenReturn(mockOrders);

        List<Order> result = ordersRepository.getPendingSellOrdersForTicker(ticker);

        assertEquals(2, result.size());
    }

    /**
     * Tests update with no affected rows
     */
    @Test
    void updateOrderStatus_shouldReturnZeroWhenOrderNotFound() {
        Long orderId = 999L;
        Order.OrderStatus newStatus = Order.OrderStatus.CANCELLED;

        when(ordersMapper.updateOrderStatus(orderId, newStatus)).thenReturn(0);

        int result = ordersRepository.updateOrderStatus(orderId, newStatus);

        assertEquals(0, result);
    }
}
