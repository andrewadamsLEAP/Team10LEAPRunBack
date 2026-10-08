package com.example.mappers;

import com.example.entities.Holding;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface HoldingsMapper {
    @Results({
            @Result(property = "client_Id", column = "client_id"),
            @Result(property = "ticker", column = "ticker"),
            @Result(property = "quantity", column = "quantity")
    })
    @Select("""
        SELECT *
        FROM holdings
        WHERE client_id = #{client_id} AND ticker = #{ticker}
        """)
    Holding getHoldingsByClientAndTicker(@Param("client_id") Long client_id, @Param("ticker") String ticker);
    

    @Select("""
            SELECT quantity 
            FROM holdings 
            WHERE client_id = #{client_id} AND ticker = #{ticker}
            """)
    Integer getQuantityByClientAndTicker(@Param("client_id") Long client_id, @Param("ticker") String ticker);


    @Results({
            @Result(property = "client_Id", column = "client_id"),
            @Result(property = "ticker", column = "ticker"),
            @Result(property = "quantity", column = "quantity")
    })
    @Select("""
        SELECT *
        FROM holdings
        WHERE client_id = #{client_id}
        """)
    List<Holding> getHoldingsByClient(@Param("client_id") Long client_id);



    @Insert("""
        INSERT INTO holdings (
            client_id,
            ticker,
            quantity
        )
        VALUES (
            #{holding.client_id},
            #{holding.ticker},
            #{holding.quantity}
        )
        """)
    void createHolding(@Param("holding") Holding holding);


    @Update("""
        UPDATE holdings
        SET quantity = quantity + #{quantity}
        WHERE client_id = #{client_id} AND ticker = #{ticker}
        """)
    int updateBuyHolding(@Param("quantity") Integer quantity, @Param("client_id") Long client_id, @Param("ticker") String ticker);


    @Update("""
        UPDATE holdings
        SET quantity = quantity - #{quantity}
        WHERE client_id = #{client_id} AND ticker = #{ticker}
        """)
    int updateSellHolding(@Param("quantity") Integer quantity, @Param("client_id") Long client_id, @Param("ticker") String ticker);
    
}
