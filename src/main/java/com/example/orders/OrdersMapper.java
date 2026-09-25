package com.example.orders;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface OrdersMapper {

    @Select("""
        SELECT *
        FROM orders
        WHERE order_id = #{orderId}
        """)
    Order getOrderById(@Param("orderId") Long orderId);


    @Select("""
        SELECT *
        FROM orders
        WHERE order_status = 'FULFILLED'
          AND client_id = #{clientId}
        """)
    List<Order> getFulfilledOrders(
            @Param("clientId") Long clientId
    );


    @Select("""
        SELECT *
        FROM orders
        WHERE order_status = 'CANCELLED'
        """)
    List<Order> getCancelledOrders();


    @Select("""
        SELECT *
        FROM orders
        WHERE order_status = 'PENDING'
          AND ticker = #{ticker}
          AND order_type = 'SELL'
        """)
    List<Order> getPendingSellOrdersForTicker(
            @Param("ticker") String ticker
    );


    @Select("""
        SELECT *
        FROM orders
        WHERE order_status = 'PENDING'
          AND ticker = #{ticker}
          AND order_type = 'BUY'
        """)
    List<Order> getPendingBuyOrdersForTicker(
            @Param("ticker") String ticker
    );


    @Insert("""
        INSERT INTO orders (
            client_id,
            ticker,
            order_type,
            order_status,
            price,
            quantity,
            order_date
        )
        VALUES (
            #{order.clientId},
            #{order.ticker},
            #{order.orderType},
            #{order.orderStatus},
            #{order.price},
            #{order.quantity},
            #{order.orderDate}
        )
        """)
    void createOrder(@Param("order") Order order);


    @Update("""
        UPDATE orders
        SET order_status = #{status}
        WHERE order_id = #{orderId}
        """)
    int updateOrderStatus(
            @Param("orderId") Long orderId,
            @Param("status") Order.OrderStatus status
    );
}