package com.example.mappers;

import org.apache.ibatis.annotations.*;

import com.example.entities.Transaction;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface TransactionsMapper {

    @Select("""
        SELECT
            transaction_id,
            client_id,
            type,
            amount,
            timestamp
        FROM transactions
        WHERE client_id = #{clientId}
        ORDER BY timestamp DESC
        """)
    List<Transaction> getTransactions(
            @Param("clientId") Long clientId
    );

    @Update("""
        UPDATE clients
        SET buying_power = buying_power + #{amount}
        WHERE client_id = #{clientId}
        """)
    int increaseBuyingPower(
            @Param("clientId") Long clientId,
            @Param("amount") BigDecimal amount
    );

    @Update("""
        UPDATE clients
        SET buying_power = buying_power - #{amount}
        WHERE client_id = #{clientId}
        """)
    int decreaseBuyingPower(
            @Param("clientId") Long clientId,
            @Param("amount") BigDecimal amount
    );

    @Insert("""
        INSERT INTO transactions (
            client_id,
            type,
            amount,
            timestamp
        )
        VALUES (
            #{clientId},
            #{type},
            #{amount},
            #{timestamp}
        )
        """)
    @Options(
        useGeneratedKeys = true,
        keyProperty = "transactionId"
    )
    int saveTransaction(Transaction transaction);
}

