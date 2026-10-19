package com.lpms.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lpms.model.EmployeeSalaryDetail;
import com.lpms.util.DBUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Data Access Object for Module 7: Administration & Configuration.
 * Manages Departments, Designations, Leave Types, Salary Components, Tax Slabs,
 * and Employee Salary Master Structures.
 */
public class AdminConfigDAO {

    // =========================================================================
    // 1. DEPARTMENTS
    // =========================================================================

    public JsonArray listDepartments() throws SQLException {
        String sql = "SELECT department_id, department_code, department_name, description, is_active, " +
                     "(SELECT COUNT(*) FROM employee e WHERE e.department_id = d.department_id) AS employee_count " +
                     "FROM department d ORDER BY department_name ASC";
        JsonArray arr = new JsonArray();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                JsonObject o = new JsonObject();
                o.addProperty("departmentId", rs.getLong("department_id"));
                o.addProperty("departmentCode", rs.getString("department_code"));
                o.addProperty("departmentName", rs.getString("department_name"));
                o.addProperty("description", rs.getString("description"));
                o.addProperty("isActive", rs.getBoolean("is_active"));
                o.addProperty("employeeCount", rs.getInt("employee_count"));
                arr.add(o);
            }
        }
        return arr;
    }

    public long createDepartment(String code, String name, String desc) throws SQLException {
        String sql = "INSERT INTO department (department_code, department_name, description, is_active) VALUES (?, ?, ?, TRUE)";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            st.setString(1, code.trim().toUpperCase());
            st.setString(2, name.trim());
            if (desc == null || desc.isBlank()) st.setNull(3, Types.VARCHAR); else st.setString(3, desc.trim());
            st.executeUpdate();
            try (ResultSet rs = st.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return -1L;
    }

    public boolean updateDepartment(long id, String name, String desc) throws SQLException {
        String sql = "UPDATE department SET department_name = ?, description = ? WHERE department_id = ?";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, name.trim());
            if (desc == null || desc.isBlank()) st.setNull(2, Types.VARCHAR); else st.setString(2, desc.trim());
            st.setLong(3, id);
            return st.executeUpdate() > 0;
        }
    }

    public boolean toggleDepartmentStatus(long id, boolean active) throws SQLException {
        String sql = "UPDATE department SET is_active = ? WHERE department_id = ?";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setBoolean(1, active);
            st.setLong(2, id);
            return st.executeUpdate() > 0;
        }
    }

    // =========================================================================
    // 2. DESIGNATIONS
    // =========================================================================

    public JsonArray listDesignations() throws SQLException {
        String sql = "SELECT designation_id, designation_code, designation_name, job_level, description, is_active, " +
                     "(SELECT COUNT(*) FROM employee e WHERE e.designation_id = d.designation_id) AS employee_count " +
                     "FROM designation d ORDER BY designation_name ASC";
        JsonArray arr = new JsonArray();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                JsonObject o = new JsonObject();
                o.addProperty("designationId", rs.getLong("designation_id"));
                o.addProperty("designationCode", rs.getString("designation_code"));
                o.addProperty("designationName", rs.getString("designation_name"));
                o.addProperty("jobLevel", rs.getString("job_level"));
                o.addProperty("description", rs.getString("description"));
                o.addProperty("isActive", rs.getBoolean("is_active"));
                o.addProperty("employeeCount", rs.getInt("employee_count"));
                arr.add(o);
            }
        }
        return arr;
    }

    public long createDesignation(String code, String name, String jobLevel, String desc) throws SQLException {
        String sql = "INSERT INTO designation (designation_code, designation_name, job_level, description, is_active) VALUES (?, ?, ?, ?, TRUE)";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            st.setString(1, code.trim().toUpperCase());
            st.setString(2, name.trim());
            st.setString(3, jobLevel != null ? jobLevel.trim() : "Mid");
            if (desc == null || desc.isBlank()) st.setNull(4, Types.VARCHAR); else st.setString(4, desc.trim());
            st.executeUpdate();
            try (ResultSet rs = st.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return -1L;
    }

    public boolean updateDesignation(long id, String name, String jobLevel, String desc) throws SQLException {
        String sql = "UPDATE designation SET designation_name = ?, job_level = ?, description = ? WHERE designation_id = ?";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, name.trim());
            st.setString(2, jobLevel != null ? jobLevel.trim() : "Mid");
            if (desc == null || desc.isBlank()) st.setNull(3, Types.VARCHAR); else st.setString(3, desc.trim());
            st.setLong(4, id);
            return st.executeUpdate() > 0;
        }
    }

    public boolean toggleDesignationStatus(long id, boolean active) throws SQLException {
        String sql = "UPDATE designation SET is_active = ? WHERE designation_id = ?";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setBoolean(1, active);
            st.setLong(2, id);
            return st.executeUpdate() > 0;
        }
    }

    // =========================================================================
    // 3. LEAVE TYPES
    // =========================================================================

    public JsonArray listLeaveTypes() throws SQLException {
        String sql = "SELECT * FROM leave_type ORDER BY leave_type_id ASC";
        JsonArray arr = new JsonArray();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                JsonObject o = new JsonObject();
                o.addProperty("leaveTypeId", rs.getLong("leave_type_id"));
                o.addProperty("leaveCode", rs.getString("leave_code"));
                o.addProperty("leaveName", rs.getString("leave_name"));
                o.addProperty("description", rs.getString("description"));
                o.addProperty("annualEntitlement", rs.getBigDecimal("annual_entitlement"));
                o.addProperty("isPaid", rs.getBoolean("is_paid"));
                o.addProperty("allowsHalfDay", rs.getBoolean("allows_half_day"));
                o.addProperty("allowsCarryForward", rs.getBoolean("allows_carry_forward"));
                o.addProperty("maxCarryForward", rs.getBigDecimal("max_carry_forward"));
                o.addProperty("isActive", rs.getBoolean("is_active"));
                arr.add(o);
            }
        }
        return arr;
    }

    public long createLeaveType(String code, String name, String desc, BigDecimal entitlement,
                                boolean isPaid, boolean allowsHalfDay, boolean allowsCarryForward, BigDecimal maxCarry) throws SQLException {
        String sql = "INSERT INTO leave_type (leave_code, leave_name, description, annual_entitlement, " +
                     "is_paid, allows_half_day, allows_carry_forward, max_carry_forward, is_active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE)";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            st.setString(1, code.trim().toUpperCase());
            st.setString(2, name.trim());
            if (desc == null || desc.isBlank()) st.setNull(3, Types.VARCHAR); else st.setString(3, desc.trim());
            st.setBigDecimal(4, entitlement != null ? entitlement : new BigDecimal("12.00"));
            st.setBoolean(5, isPaid);
            st.setBoolean(6, allowsHalfDay);
            st.setBoolean(7, allowsCarryForward);
            st.setBigDecimal(8, maxCarry != null ? maxCarry : BigDecimal.ZERO);
            st.executeUpdate();
            try (ResultSet rs = st.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return -1L;
    }

    public boolean updateLeaveType(long id, String name, BigDecimal entitlement, boolean isPaid,
                                   boolean allowsHalfDay, boolean allowsCarryForward, BigDecimal maxCarry, boolean isActive) throws SQLException {
        String sql = "UPDATE leave_type SET leave_name = ?, annual_entitlement = ?, is_paid = ?, " +
                     "allows_half_day = ?, allows_carry_forward = ?, max_carry_forward = ?, is_active = ? " +
                     "WHERE leave_type_id = ?";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, name.trim());
            st.setBigDecimal(2, entitlement != null ? entitlement : new BigDecimal("12.00"));
            st.setBoolean(3, isPaid);
            st.setBoolean(4, allowsHalfDay);
            st.setBoolean(5, allowsCarryForward);
            st.setBigDecimal(6, maxCarry != null ? maxCarry : BigDecimal.ZERO);
            st.setBoolean(7, isActive);
            st.setLong(8, id);
            return st.executeUpdate() > 0;
        }
    }

    // =========================================================================
    // 4. SALARY COMPONENTS
    // =========================================================================

    public JsonArray listSalaryComponents() throws SQLException {
        String sql = "SELECT * FROM salary_component ORDER BY component_id ASC";
        JsonArray arr = new JsonArray();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                JsonObject o = new JsonObject();
                o.addProperty("componentId", rs.getLong("component_id"));
                o.addProperty("componentCode", rs.getString("component_code"));
                o.addProperty("componentName", rs.getString("component_name"));
                o.addProperty("componentType", rs.getString("component_type"));
                o.addProperty("calculationMethod", rs.getString("calculation_method"));
                o.addProperty("isTaxable", rs.getBoolean("is_taxable"));
                o.addProperty("isPfApplicable", rs.getBoolean("is_pf_applicable"));
                o.addProperty("isEsiApplicable", rs.getBoolean("is_esi_applicable"));
                o.addProperty("isActive", rs.getBoolean("is_active"));
                arr.add(o);
            }
        }
        return arr;
    }

    public long createSalaryComponent(String code, String name, String type, String method,
                                      boolean isTaxable, boolean isPf) throws SQLException {
        String sql = "INSERT INTO salary_component (component_code, component_name, component_type, " +
                     "calculation_method, is_taxable, is_pf_applicable, is_active) VALUES (?, ?, ?, ?, ?, ?, TRUE)";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            st.setString(1, code.trim().toUpperCase());
            st.setString(2, name.trim());
            st.setString(3, type.trim().toUpperCase());
            st.setString(4, method.trim().toUpperCase());
            st.setBoolean(5, isTaxable);
            st.setBoolean(6, isPf);
            st.executeUpdate();
            try (ResultSet rs = st.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return -1L;
    }

    public boolean updateSalaryComponent(long id, String name, String type, String method,
                                         boolean isTaxable, boolean isPf, boolean active) throws SQLException {
        String sql = "UPDATE salary_component SET component_name = ?, component_type = ?, " +
                     "calculation_method = ?, is_taxable = ?, is_pf_applicable = ?, is_active = ? " +
                     "WHERE component_id = ?";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, name.trim());
            st.setString(2, type.trim().toUpperCase());
            st.setString(3, method.trim().toUpperCase());
            st.setBoolean(4, isTaxable);
            st.setBoolean(5, isPf);
            st.setBoolean(6, active);
            st.setLong(7, id);
            return st.executeUpdate() > 0;
        }
    }

    // =========================================================================
    // 5. TAX SLABS
    // =========================================================================

    public JsonArray listTaxSlabs() throws SQLException {
        String sql = "SELECT * FROM tax_slab ORDER BY tax_regime ASC, slab_order ASC";
        JsonArray arr = new JsonArray();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                JsonObject o = new JsonObject();
                o.addProperty("taxSlabId", rs.getLong("tax_slab_id"));
                o.addProperty("taxRegime", rs.getString("tax_regime"));
                o.addProperty("financialYear", rs.getString("financial_year"));
                o.addProperty("slabOrder", rs.getInt("slab_order"));
                o.addProperty("lowerLimit", rs.getBigDecimal("lower_limit"));
                BigDecimal upper = rs.getBigDecimal("upper_limit");
                o.addProperty("upperLimit", rs.wasNull() ? null : upper);
                o.addProperty("taxRate", rs.getBigDecimal("tax_rate"));
                o.addProperty("cessRate", rs.getBigDecimal("cess_rate"));
                arr.add(o);
            }
        }
        return arr;
    }

    public long saveTaxSlab(String regime, String finYear, int order, BigDecimal lower,
                            BigDecimal upper, BigDecimal rate, BigDecimal cess) throws SQLException {
        String sql = "INSERT INTO tax_slab (tax_regime, financial_year, slab_order, lower_limit, upper_limit, tax_rate, cess_rate) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE lower_limit = VALUES(lower_limit), upper_limit = VALUES(upper_limit), " +
                     "tax_rate = VALUES(tax_rate), cess_rate = VALUES(cess_rate)";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            st.setString(1, regime.trim().toUpperCase());
            st.setString(2, finYear.trim());
            st.setInt(3, order);
            st.setBigDecimal(4, lower);
            if (upper == null) st.setNull(5, Types.DECIMAL); else st.setBigDecimal(5, upper);
            st.setBigDecimal(6, rate);
            st.setBigDecimal(7, cess != null ? cess : new BigDecimal("4.00"));
            st.executeUpdate();
            try (ResultSet rs = st.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return -1L;
    }

    // =========================================================================
    // 6. EMPLOYEE SALARY MASTER & STRUCTURE
    // =========================================================================

    public JsonObject getEmployeeSalary(long employeeId) throws SQLException {
        String sql = "SELECT employee_salary_id, annual_ctc, monthly_gross, effective_from, is_active " +
                     "FROM employee_salary WHERE employee_id = ? AND is_active = TRUE LIMIT 1";

        JsonObject out = null;
        long salId = -1L;
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setLong(1, employeeId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    out = new JsonObject();
                    salId = rs.getLong("employee_salary_id");
                    out.addProperty("employeeSalaryId", salId);
                    out.addProperty("employeeId", employeeId);
                    out.addProperty("annualCtc", rs.getBigDecimal("annual_ctc"));
                    out.addProperty("monthlyGross", rs.getBigDecimal("monthly_gross"));
                    out.addProperty("effectiveFrom", rs.getDate("effective_from").toString());
                    out.addProperty("isActive", rs.getBoolean("is_active"));
                }
            }

            if (out != null && salId > 0) {
                String detSql = "SELECT esd.salary_detail_id, esd.component_id, esd.component_value, " +
                                "sc.component_code, sc.component_name, sc.component_type, sc.calculation_method " +
                                "FROM employee_salary_detail esd " +
                                "JOIN salary_component sc ON sc.component_id = esd.component_id " +
                                "WHERE esd.employee_salary_id = ? ORDER BY sc.component_type ASC, sc.component_id ASC";
                JsonArray items = new JsonArray();
                try (PreparedStatement detSt = c.prepareStatement(detSql)) {
                    detSt.setLong(1, salId);
                    try (ResultSet rs = detSt.executeQuery()) {
                        while (rs.next()) {
                            JsonObject item = new JsonObject();
                            item.addProperty("salaryDetailId", rs.getLong("salary_detail_id"));
                            item.addProperty("componentId", rs.getLong("component_id"));
                            item.addProperty("componentCode", rs.getString("component_code"));
                            item.addProperty("componentName", rs.getString("component_name"));
                            item.addProperty("componentType", rs.getString("component_type"));
                            item.addProperty("calculationMethod", rs.getString("calculation_method"));
                            item.addProperty("componentValue", rs.getBigDecimal("component_value"));
                            items.add(item);
                        }
                    }
                }
                out.add("components", items);
            }
        }
        return out;
    }

    public boolean saveEmployeeSalary(long employeeId, BigDecimal annualCtc, BigDecimal monthlyGross,
                                      List<EmployeeSalaryDetail> details) throws SQLException {
        Connection c = null;
        try {
            c = DBUtil.getConnection();
            c.setAutoCommit(false);

            // Deactivate previous salaries
            try (PreparedStatement deactSt = c.prepareStatement(
                    "UPDATE employee_salary SET is_active = FALSE, effective_to = CURRENT_DATE WHERE employee_id = ? AND is_active = TRUE")) {
                deactSt.setLong(1, employeeId);
                deactSt.executeUpdate();
            }

            // Insert new salary record
            long newSalId;
            String insSalSql = "INSERT INTO employee_salary (employee_id, annual_ctc, monthly_gross, effective_from, is_active) " +
                               "VALUES (?, ?, ?, CURRENT_DATE, TRUE)";
            try (PreparedStatement insSt = c.prepareStatement(insSalSql, Statement.RETURN_GENERATED_KEYS)) {
                insSt.setLong(1, employeeId);
                insSt.setBigDecimal(2, annualCtc);
                insSt.setBigDecimal(3, monthlyGross);
                insSt.executeUpdate();
                try (ResultSet rs = insSt.getGeneratedKeys()) {
                    if (!rs.next()) throw new SQLException("Failed to create employee salary record.");
                    newSalId = rs.getLong(1);
                }
            }

            // Insert details
            if (details != null && !details.isEmpty()) {
                String insDetSql = "INSERT INTO employee_salary_detail (employee_salary_id, component_id, component_value) VALUES (?, ?, ?)";
                try (PreparedStatement detSt = c.prepareStatement(insDetSql)) {
                    for (EmployeeSalaryDetail d : details) {
                        detSt.setLong(1, newSalId);
                        detSt.setLong(2, d.getComponentId());
                        detSt.setBigDecimal(3, d.getComponentValue() != null ? d.getComponentValue() : BigDecimal.ZERO);
                        detSt.addBatch();
                    }
                    detSt.executeBatch();
                }
            }

            c.commit();
            return true;
        } catch (SQLException e) {
            if (c != null) try { c.rollback(); } catch (SQLException ignored) {}
            throw e;
        } finally {
            if (c != null) {
                try { c.setAutoCommit(true); } catch (SQLException ignored) {}
                DBUtil.closeConnection(c);
            }
        }
    }
}
