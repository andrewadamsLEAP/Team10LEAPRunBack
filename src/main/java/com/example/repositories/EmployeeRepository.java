package com.example.repositories;

import com.example.DTOs.clients.LoginView;
import com.example.entities.Employee;
import com.example.mappers.EmployeeMapper;
import org.springframework.stereotype.Repository;

@Repository
public class EmployeeRepository {
    private final EmployeeMapper employeeMapper;

    public EmployeeRepository(EmployeeMapper employeeMapper) {
        this.employeeMapper = employeeMapper;
    }

    public Employee findEmployeeById(Long id) {
        return employeeMapper.findById(id);
    }

    public Long findEmployeeIdByEmail(String email) {
        return employeeMapper.findEmployeeIdByEmail(email);
    }

    public Long findEmployeeIdByUsername(String username) {
        return employeeMapper.findEmployeeIdByUsername(username);
    }

    public LoginView findLoginEmployeeByUsername(String username) {
        return employeeMapper.findLoginEmployeeByUsername(username);
    }

    public LoginView findLoginEmployeeById(Long employeeId) {
        return employeeMapper.findLoginEmployeeById(employeeId);
    }

    public int updatePassword(Long employeeId, String hashedPassword) {
        return employeeMapper.updatePassword(employeeId, hashedPassword);
    }
}
