package com.example.services;

import org.springframework.stereotype.Service;

import com.example.DTOs.employees.EmployeeProfileView;
import com.example.entities.*;
import com.example.exceptions.Validate;
import com.example.repositories.EmployeeRepository;

import java.util.List;

@Service 
public class EmployeeService {
    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository)
    {
        this.employeeRepository = employeeRepository;
    }



    public EmployeeProfileView getEmployeeProfile(Long employeeId) {
        Employee employee = Validate.validateUser(employeeRepository.findEmployeeById(employeeId), "employee");

        return new EmployeeProfileView(
                employee.getEmployeeId(),
                employee.getEmail(),
                employee.getUsername(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getRole());
    }

    /**
     * Returns the reporter accounts visible to an authenticated administrator.
     */
    public List<EmployeeProfileView> getAllReporters() {
        return employeeRepository.findAllReporters();
    }
   

}
