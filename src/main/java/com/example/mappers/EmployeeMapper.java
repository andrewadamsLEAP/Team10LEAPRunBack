package com.example.mappers;

import com.example.DTOs.clients.LoginView;
import com.example.entities.Employee;
import org.apache.ibatis.annotations.*;

@Mapper
public interface EmployeeMapper {

    @Select("""
        SELECT employee_id AS employeeId,
               email,
               username,
               password,
               first_name AS firstName,
               last_name AS lastName,
               role
        FROM employees
        WHERE employee_id = #{employeeId}
        """)
    Employee findById(@Param("employeeId") Long employeeId);

    @Select("""
        SELECT employee_id
        FROM employees
        WHERE email = #{email}
        """)
    Long findEmployeeIdByEmail(@Param("email") String email);

    @Select("""
        SELECT employee_id
        FROM employees
        WHERE username = #{username}
        """)
    Long findEmployeeIdByUsername(@Param("username") String username);

    @Select("""
        SELECT employee_id AS userId,
               username,
               password
        FROM employees
        WHERE username = #{username}
        """)
    LoginView findLoginEmployeeByUsername(@Param("username") String username);

    @Select("""
        SELECT employee_id AS userId,
               username,
               password
        FROM employees
        WHERE employee_id = #{employeeId}
        """)
    LoginView findLoginEmployeeById(@Param("employeeId") Long employeeId);

    @Update("""
        UPDATE employees
        SET password = #{hashedPassword}
        WHERE employee_id = #{employeeId}
        """)
    int updatePassword(
        @Param("employeeId") Long employeeId,
        @Param("hashedPassword") String hashedPassword
    );
}