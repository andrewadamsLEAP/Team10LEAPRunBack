package com.example.mappers;

import com.example.DTOs.clients.LoginView;
import com.example.DTOs.employees.EmployeeLoginView;
import com.example.DTOs.employees.EmployeeProfileView;
import com.example.entities.Employee;
import org.apache.ibatis.annotations.*;

import java.util.List;

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

    /**
     * Finds all employees whose stored role identifies them as reporters.
     * The projection intentionally excludes password data.
     */
    @Select("""
        SELECT employee_id AS employeeId,
               email,
               username,
               first_name AS firstName,
               last_name AS lastName,
               role
        FROM employees
        WHERE UPPER(role) IN ('REPORTER', 'EMPLOYEE_REPORTER')
        ORDER BY employee_id
        """)
    List<EmployeeProfileView> findAllReporters();

    @Select("""
        SELECT employee_id
        FROM employees
        WHERE LOWER(email) = LOWER(#{email})
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
               password,
               role
        FROM employees
        WHERE username = #{username}
        """)
    EmployeeLoginView findLoginEmployeeByUsername(@Param("username") String username);

    @Select("""
        SELECT employee_id AS userId,
               username,
               password,
               role
        FROM employees
        WHERE username = #{identifier} OR email = #{identifier}
        """)
    EmployeeLoginView findLoginEmployeeByIdentifier(@Param("identifier") String identifier);

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