package com.example.mappers;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.entities.Order;

import java.util.List;

@Mapper
public interface OrdersMapper {

    /**
     * Retrieves an order by its ID.
     *
     * @param orderId the ID of the order to retrieve
     * @return the order with the specified ID, or null if not found
     */
    @Select("""
        SELECT
            order_id,
            client_id,
            ticker,
            order_type,
            order_status,
            quantity,
            price,
            order_date
        FROM orders
        WHERE order_id = #{orderId}
        """)
    Order getOrderById(@Param("orderId") Long orderId);


    /**
     * Retrieves all fulfilled orders for a specific client.
     *
     * @param clientId the ID of the client
     * @return a list of fulfilled orders for the specified client
     */
    @Select("""
        SELECT
            order_id,
            client_id,
            ticker,
            order_type,
            order_status,
            quantity,
            price,
            order_date
        FROM orders
        WHERE order_status = 'FULFILLED'
          AND client_id = #{clientId}
        """)
    List<Order> getFulfilledOrders(
            @Param("clientId") Long clientId
    );

    /**
     * Retrieves all cancelled orders.
     *
     * @return a list of all cancelled orders
     */
    @Select("""
        SELECT
            order_id,
            client_id,
            ticker,
            order_type,
            order_status,
            quantity,
            price,
            order_date
        FROM orders
        WHERE order_status = 'CANCELLED'
        """)
    List<Order> getCancelledOrders();


    /**
     * Retrieves all cancelled orders for a specific client.
     *
     * @param clientId the ID of the client
     * @return a list of cancelled orders for the specified client
     */
    @Select("""
        SELECT
            order_id,
            client_id,
            ticker,
            order_type,
            order_status,
            quantity,
            price,
            order_date
        FROM orders
        WHERE order_status = 'CANCELLED'
          AND client_id = #{clientId}
        """)
    List<Order> getCancelledOrdersForClient(
            @Param("clientId") Long clientId
    );


    /**
     * Retrieves all pending sell orders for a specific ticker.
     *
     * @param ticker the ticker symbol
     * @return a list of pending sell orders for the specified ticker
     */
    @Select("""
        SELECT
            order_id,
            client_id,
            ticker,
            order_type,
            order_status,
            quantity,
            price,
            order_date
        FROM orders
        WHERE order_status = 'PENDING'
          AND ticker = #{ticker}
          AND order_type = 'SELL'
        """)
    List<Order> getPendingSellOrdersForTicker(
            @Param("ticker") String ticker
    );

    /**
     * Retrieves all pending buy orders for a specific ticker.
     *
     * @param ticker the ticker symbol
     * @return a list of pending buy orders for the specified ticker
     */
    @Select("""
        SELECT
            order_id,
            client_id,
            ticker,
            order_type,
            order_status,
            quantity,
            price,
            order_date
        FROM orders
        WHERE order_status = 'PENDING'
          AND ticker = #{ticker}
          AND order_type = 'BUY'
        """)
    List<Order> getPendingBuyOrdersForTicker(
            @Param("ticker") String ticker
    );


    /**
     * Retrieves all pending buy orders for a specific client.
     *
     * @param clientId the ID of the client
     * @return a list of pending buy orders for the specified client
     */
    @Select("""
        SELECT
            order_id,
            client_id,
            ticker,
            order_type,
            order_status,
            quantity,
            price,
            order_date
        FROM orders
        WHERE order_status = 'PENDING'
          AND client_id = #{clientId}
          AND order_type = 'BUY'
        """)
    List<Order> getPendingBuyOrdersForClient(
            @Param("clientId") Long clientId
    );

    /**
     * Retrieves all pending sell orders for a specific client.
     *
     * @param clientId the ID of the client
     * @return a list of pending sell orders for the specified client
     */
    @Select("""
        SELECT
            order_id,
            client_id,
            ticker,
            order_type,
            order_status,
            quantity,
            price,
            order_date
        FROM orders
        WHERE order_status = 'PENDING'
          AND client_id = #{clientId}
          AND order_type = 'SELL'
        """)
    List<Order> getPendingSellOrdersForClient(
            @Param("clientId") Long clientId
    );

    /**
     * Retrieves all pending sell orders for a specific client and ticker.
     *
     * @param clientId the ID of the client
     * @param ticker the ticker symbol
     * @return a list of pending sell orders for the specified client and ticker
     */
    @Select("""
        SELECT
            order_id,
            client_id,
            ticker,
            order_type,
            order_status,
            quantity,
            price,
            order_date
        FROM orders
        WHERE order_status = 'PENDING'
          AND client_id = #{clientId}
          AND ticker = #{ticker}
          AND order_type = 'SELL'
        """)
    List<Order> getPendingSellOrdersForClientAndTicker(
            @Param("clientId") Long clientId,
            @Param("ticker") String ticker
    );


    /**
     * Creates a new order in the database.
     *
     * @param order the order to create
     */
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
    @Options(useGeneratedKeys = true, keyProperty = "order.orderId")
    void createOrder(@Param("order") Order order);


    /**
     * Updates the status of an existing order.
     *
     * @param orderId the ID of the order to update
     * @param status the new status of the order
     * @return the number of rows affected
     */
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
