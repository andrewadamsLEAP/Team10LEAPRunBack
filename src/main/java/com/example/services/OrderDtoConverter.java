package com.example.services;

import com.example.entities.Order;
import com.example.DTOs.orders.OrderResponse;
import com.example.DTOs.orders.OrderHistoryView;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderDtoConverter {

    /**
     * Convert Order entity to OrderResponse DTO
     */
    public OrderResponse toOrderResponse(Order order) {
        return new OrderResponse(
                order.getOrderId(),
                order.getTicker(),
                order.getQuantity(),
                order.getPrice(),
                order.getOrderStatus().name(),
                order.getOrderDate().toLocalDateTime()
        );
    }

    /**
     * Convert Order entity to OrderHistoryView DTO
     */
    public OrderHistoryView toOrderHistoryView(Order order) {
        return new OrderHistoryView(
                order.getOrderId(),
                order.getTicker(),
                order.getQuantity(),
                order.getPrice(),
                order.getOrderStatus().name(),
                order.getOrderDate().toLocalDateTime(),
                null,  // executedPrice - would come from execution details if tracked
                null,  // executedAt - would come from execution details if tracked
                null   // cancellationReason - would come from cancellation tracking if implemented
        );
    }

    /**
     * Convert list of Order entities to OrderResponse DTOs
     */
    public List<OrderResponse> toOrderResponses(List<Order> orders) {
        return orders.stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert list of Order entities to OrderHistoryView DTOs
     */
    public List<OrderHistoryView> toOrderHistoryViews(List<Order> orders) {
        return orders.stream()
                .map(this::toOrderHistoryView)
                .collect(Collectors.toList());
    }

}
