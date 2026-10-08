package com.example.mappers;

import com.example.DTOs.clients.LoginView;
import com.example.entities.Employee;
import org.apache.ibatis.annotations.*;

@Mapper
public interface EmployeeMapper {
    
    @Select("SELECT * FROM employees WHERE employee_id = #{employeeId}")
    Employee findById(@Param("employeeId") Long employeeId);
    
    @Select("SELECT employee_id FROM employees WHERE email = #{email}")
    Long findEmployeeIdByEmail(@Param("email") String email);
    
    @Select("SELECT employee_id FROM employees WHERE username = #{username}")
    Long findEmployeeIdByUsername(@Param("username") String username);
    
    @Select("SELECT employee_id AS employeeId, username, email FROM employees WHERE username = #{username}")
    LoginView findLoginEmployeeByUsername(@Param("username") String username);
    
    @Select("SELECT employee_id AS employeeId, username, email FROM employees WHERE employee_id = #{employeeId}")
    LoginView findLoginEmployeeById(@Param("employeeId") Long employeeId);
    
    @Update("UPDATE employees SET password = #{hashedPassword} WHERE employee_id = #{employeeId}")
    int updatePassword(@Param("employeeId") Long employeeId, @Param("hashedPassword") String hashedPassword);
    
    @Insert("INSERT INTO employees (email, username, password) VALUES (#{email}, #{username}, #{password})")
    void createEmployee(Employee employee);
}
