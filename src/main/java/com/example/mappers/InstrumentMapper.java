package com.example.mappers;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import com.example.entities.Instrument;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InstrumentMapper {
    @Select("""
        SELECT ticker, previous_close, open, volume,
               avg_volume, asset_type
        FROM instruments
        ORDER BY ticker
        """)
    @Results({
        @Result(property = "ticker", column = "ticker"),
        @Result(property = "previousClose", column = "previous_close"),
        @Result(property = "open", column = "open"),
        @Result(property = "volume", column = "volume"),
        @Result(property = "avgVolume", column = "avg_volume"),
        @Result(property = "assetType", column = "asset_type")
    })
    List<Instrument> findAll();

    @Select("""
        SELECT ticker, previous_close, open, volume,
               avg_volume, asset_type
        FROM instruments
        WHERE ticker = #{ticker}
        """)
    @Results({
        @Result(property = "ticker", column = "ticker"),
        @Result(property = "previousClose", column = "previous_close"),
        @Result(property = "open", column = "open"),
        @Result(property = "volume", column = "volume"),
        @Result(property = "avgVolume", column = "avg_volume"),
        @Result(property = "assetType", column = "asset_type")
    })
    Instrument findByTicker(@Param("ticker") String ticker);
}
