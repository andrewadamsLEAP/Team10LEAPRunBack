package com.example.config;

import com.example.entities.Order;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class OrderTypeHandler extends BaseTypeHandler<Order.OrderType> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Order.OrderType parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.name());
    }

    @Override
    public Order.OrderType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? null : Order.OrderType.valueOf(value);
    }

    @Override
    public Order.OrderType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? null : Order.OrderType.valueOf(value);
    }

    @Override
    public Order.OrderType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? null : Order.OrderType.valueOf(value);
    }
}
