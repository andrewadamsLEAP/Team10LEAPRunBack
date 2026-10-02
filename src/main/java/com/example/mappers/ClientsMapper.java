package com.example.mappers;

import com.example.DTOs.clients.ClientAdminView;
import com.example.DTOs.clients.LoginView;
import com.example.DTOs.clients.ClientReporterView;
import com.example.entities.Client;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface ClientsMapper {
    @Select("""
         SELECT client_id AS clientId,
             email,
             username,
             first_name AS firstName,
             last_name AS lastName,
             cash_amount AS cashAmount
            FROM clients
            WHERE client_id = #{clientId}
            """)
    Client findById(@Param("clientId") Long clientId);

    @Select("""
     SELECT client_id AS clientId,
         username,
         email,
         first_name AS firstName,
         last_name AS lastName,
         cash_amount AS cashAmount
        FROM clients
        WHERE client_id = #{clientId}
        """)
    ClientAdminView findByIdAdmin(@Param("clientId") Long clientId);

    @Select("""
     SELECT client_id AS clientId,
         username,
         email
        FROM clients
        WHERE client_id = #{clientId}
        """)
    ClientReporterView findByIdReporter(@Param("clientId") Long clientId);

    @Select("""
         SELECT client_id AS clientId,
             username,
             email,
             first_name AS firstName,
             last_name AS lastName,
             cash_amount AS cashAmount
            FROM clients
            """)
    List<ClientAdminView> findAllClientsAdmin();

    @Select("""
         SELECT client_id AS clientId,
             username,
             email
            FROM clients
            """)
    List<ClientReporterView> findAllClientsReporter();
    
    @Select("""
            SELECT client_id
            FROM clients
            WHERE email = #{email}
            """)
    Long findClientIdByEmail(@Param("email") String email);

    @Select("""
            SELECT client_id
            FROM clients
            WHERE username = #{username}
            """)
    Long findClientIdByUsername(@Param("username") String username);

    @Select("""
            SELECT client_id AS clientId,
               username,
               password
            FROM clients
            WHERE username = #{username}
            """)
    LoginView findLoginClientByUsername(@Param("username") String username);

    @Select("""
            SELECT client_id AS clientId,
               username,
               password
            FROM clients
            WHERE client_id = #{clientId}
            """)
    LoginView findLoginClientById(@Param("clientId") Long clientId);

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
            <if test="username != null and username.trim() != ''">username = #{username},</if>
            <if test="firstName != null and firstName.trim() != ''">first_name = #{firstName},</if>
            <if test="lastName != null and lastName.trim() != ''">last_name = #{lastName},</if>
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
