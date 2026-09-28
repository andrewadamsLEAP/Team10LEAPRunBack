package com.example.clients;

import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface ClientsMapper {
    @Select("""
            SELECT client_id, username, email, first_name, last_name, cash_amount, updated_at
            FROM clients
            WHERE client_id = #{clientId}
            """)
    Client findById(@Param("clientId") Long clientId);

    @Select("""
            SELECT client_id, username, email, first_name, last_name, cash_amount, updated_at
            FROM clients
            """)
    List<Client> findAllClients();
    
    @Select("""
            SELECT client_id, username, email, first_name, last_name, cash_amount, updated_at
            FROM clients
            WHERE username = #{username}
            """)
    Client findByUsername(@Param("username") String username);

    @Select("""
            SELECT client_id, username, email, first_name, last_name, cash_amount, updated_at
            FROM clients
            WHERE username = #{username} AND password = #{password}
            """)
    Client findByUsernameAndPassword(@Param("username") String username, @Param("password") String password);

    @Insert("""
        INSERT INTO clients (
            email,
            username,
            password,
            first_name,
            last_name
        )            
        VALUES (
            #{email},
            #{username},
            #{password},
            #{firstName},
            #{lastName}
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "clientId")
    int insert(
        @Param("email") String email,
        @Param("username") String username,
        @Param("password") String password,
        @Param("firstName") String firstName,
        @Param("lastName") String lastName
    );

    @Update("""
        <script>
        UPDATE clients
        <set>
                <if test="username != null">username = #{username},</if>
                <if test="firstName != null">first_name = #{firstName},</if>
                <if test="lastName != null">last_name = #{lastName},</if>
        </set>
        WHERE client_id = #{clientId}
        </script>
        """)
    int updateProfile(
        @Param("clientId") Long clientId,
        @Param("username") String username,
        @Param("firstName") String firstName,
        @Param("lastName") String lastName
    );

    @Update("""
        UPDATE clients
        SET password = #{hashedPassword}
        WHERE client_id = #{clientId}
        """)
    int updatePassword(
        @Param("clientId") Long clientId,
        @Param("hashedPassword") String hashedPassword
    );

    @Update("""
        UPDATE clients
        SET cash_amount = #{cashAmount}
        WHERE client_id = #{clientId}
        """)
    int updateCashAmount(
        @Param("clientId") Long clientId,
        @Param("cashAmount") BigDecimal cashAmount
    );

}
