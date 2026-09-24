package com.lpms.service;

import com.lpms.dao.EmployeeDAO;
import com.lpms.model.Employee;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {
    
    private final EmployeeDAO employeeDAO;

    public AuthService() {
        this.employeeDAO = new EmployeeDAO();
    }

    /**
     * Authenticates a user against the database.
     * 
     * @param username The username input
     * @param plainPassword The plain text password input
     * @return Employee object if authenticated successfully, null otherwise
     */
    public Employee authenticate(String username, String plainPassword) {
        Employee employee = employeeDAO.findByUsername(username);
        
        if (employee != null && employee.isActive()) {
            if (BCrypt.checkpw(plainPassword, employee.getPasswordHash())) {
                return employee;
            }
        }
        return null;
    }
    
    /**
     * Hashes a plain text password using BCrypt.
     * 
     * @param plainPassword The plain text password to hash
     * @return The BCrypt hashed password
     */
    public String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }
}
