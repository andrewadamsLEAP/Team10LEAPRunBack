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
            UPPER(ttype) AS type,
            amount,
            created_at AS timestamp
        FROM transactions
        WHERE client_id = #{clientId}
        ORDER BY created_at DESC
        """)
    List<Transaction> getTransactions(
            @Param("clientId") Long clientId
    );

    @Update("""
        UPDATE clients
        SET cash_amount = cash_amount + #{amount}
        WHERE client_id = #{clientId}
        """)
    int increaseBuyingPower(
            @Param("clientId") Long clientId,
            @Param("amount") BigDecimal amount
    );

    @Update("""
        UPDATE clients
        SET cash_amount = cash_amount - #{amount}
        WHERE client_id = #{clientId}
        """)
    int decreaseBuyingPower(
            @Param("clientId") Long clientId,
            @Param("amount") BigDecimal amount
    );

    @Insert("""
        INSERT INTO transactions (
            client_id,
            ttype,
            amount,
            created_at
        )
        VALUES (
            #{clientId},
            LOWER(#{type}),
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

