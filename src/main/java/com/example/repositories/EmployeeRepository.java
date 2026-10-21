package com.example.repositories;

import com.example.DTOs.clients.LoginView;
import com.example.DTOs.employees.EmployeeLoginView;
import com.example.DTOs.employees.EmployeeProfileView;
import com.example.entities.Employee;
import com.example.mappers.EmployeeMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EmployeeRepository {
    private final EmployeeMapper employeeMapper;

    public EmployeeRepository(EmployeeMapper employeeMapper) {
        this.employeeMapper = employeeMapper;
    }

    public Employee findEmployeeById(Long id) {
        return employeeMapper.findById(id);
    }

    /**
     * Retrieves reporter accounts for the admin-facing employee endpoint.
     */
    public List<EmployeeProfileView> findAllReporters() {
        return employeeMapper.findAllReporters();
    }

    public Long findEmployeeIdByEmail(String email) {
        return employeeMapper.findEmployeeIdByEmail(email);
    }

    public Long findEmployeeIdByUsername(String username) {
        return employeeMapper.findEmployeeIdByUsername(username);
    }

    public EmployeeLoginView findLoginEmployeeByUsername(String username) {
        return employeeMapper.findLoginEmployeeByUsername(username);
    }

    public EmployeeLoginView findLoginEmployeeByIdentifier(String identifier) {
        return employeeMapper.findLoginEmployeeByIdentifier(identifier);
    }

    public LoginView findLoginEmployeeById(Long employeeId) {
        return employeeMapper.findLoginEmployeeById(employeeId);
    }

    public int updatePassword(Long employeeId, String hashedPassword) {
        return employeeMapper.updatePassword(employeeId, hashedPassword);
    }
}
