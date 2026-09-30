package com.example.mappers;

import com.example.objects.Holding;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface HoldingsMapper {
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


    @Select("""
        SELECT *
        FROM holdings
        WHERE client_id = #{client_id}
        """)
    Holding getHoldingsByClient(@Param("client_id") Long client_id);



    @Insert("""
        INSERT INTO holdings (
            client_id,
            ticker,
            quantity,
            updated_at
        )
        VALUES (
            #{holding.client_id},
            #{holding.ticker},
            #{holding.quantity},
            CURRENT_TIMESTAMP
        )
        """)
    void createHolding(@Param("holding") Holding holding);


    @Update("""
        UPDATE holdings
        SET quantity = quantity + #{quantity}, updated_at = CURRENT_TIMESTAMP
        WHERE client_id = #{client_id} AND ticker = #{ticker}
        """)
    int updateBuyHolding(@Param("quantity") Integer quantity, @Param("client_id") Long client_id, @Param("ticker") String ticker);


    @Update("""
        UPDATE holdings
        SET quantity = quantity - #{quantity}, updated_at = CURRENT_TIMESTAMP
        WHERE client_id = #{client_id} AND ticker = #{ticker}
        """)
    int updateSellHolding(@Param("quantity") Integer quantity, @Param("client_id") Long client_id, @Param("ticker") String ticker);
    
}
