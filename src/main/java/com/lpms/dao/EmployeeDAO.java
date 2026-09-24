package com.lpms.dao;

import com.lpms.model.Employee;
import com.lpms.model.Role;
import com.lpms.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmployeeDAO {

    public Employee findByUsername(String username) {
        String sql = "SELECT * FROM employee WHERE username = ? AND is_active = TRUE";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToEmployee(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Employee findById(Long employeeId) {
        String sql = "SELECT * FROM employee WHERE employee_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, employeeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToEmployee(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Employee> findAll() {
        List<Employee> employees = new ArrayList<>();
        String sql = "SELECT * FROM employee ORDER BY employee_id DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                employees.add(mapRowToEmployee(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return employees;
    }

    public boolean create(Employee employee) {
        String sql = "INSERT INTO employee (employee_code, first_name, last_name, gender, email, " +
                "date_of_joining, employment_type, department_id, designation_id, username, password_hash, role) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, employee.getEmployeeCode());
            stmt.setString(2, employee.getFirstName());
            stmt.setString(3, employee.getLastName());
            stmt.setString(4, employee.getGender());
            stmt.setString(5, employee.getEmail());
            stmt.setDate(6, new java.sql.Date(employee.getDateOfJoining().getTime()));
            stmt.setString(7, employee.getEmploymentType());
            stmt.setLong(8, employee.getDepartmentId());
            stmt.setLong(9, employee.getDesignationId());
            stmt.setString(10, employee.getUsername());
            stmt.setString(11, employee.getPasswordHash());
            stmt.setString(12, employee.getRole().name());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Employee mapRowToEmployee(ResultSet rs) throws SQLException {
        Employee emp = new Employee();
        emp.setEmployeeId(rs.getLong("employee_id"));
        emp.setEmployeeCode(rs.getString("employee_code"));
        emp.setFirstName(rs.getString("first_name"));
        emp.setLastName(rs.getString("last_name"));
        emp.setGender(rs.getString("gender"));
        emp.setDateOfBirth(rs.getDate("date_of_birth"));
        emp.setEmail(rs.getString("email"));
        emp.setPhone(rs.getString("phone"));
        emp.setDateOfJoining(rs.getDate("date_of_joining"));
        emp.setDateOfExit(rs.getDate("date_of_exit"));
        emp.setEmploymentType(rs.getString("employment_type"));
        emp.setEmploymentStatus(rs.getString("employment_status"));
        emp.setDepartmentId(rs.getLong("department_id"));
        emp.setDesignationId(rs.getLong("designation_id"));
        emp.setReportingManagerId(rs.getLong("reporting_manager_id"));
        emp.setUsername(rs.getString("username"));
        emp.setPasswordHash(rs.getString("password_hash"));
        emp.setRole(Role.valueOf(rs.getString("role")));
        emp.setActive(rs.getBoolean("is_active"));
        emp.setLastLoginAt(rs.getTimestamp("last_login_at"));
        return emp;
    }
}
