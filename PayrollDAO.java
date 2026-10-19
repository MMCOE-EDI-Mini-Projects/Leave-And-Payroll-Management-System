package com.lpms.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lpms.model.SalaryComponent;
import com.lpms.model.TaxSlab;
import com.lpms.util.DBUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC Data Access Object for Payroll operations:
 * Handles database interaction for payroll_run, employee_payroll, payroll_item,
 * employee_salary, employee_salary_detail, and salary_component.
 * Uses PreparedStatements throughout to guarantee SQL injection protection.
 */
public class PayrollDAO {

    /**
     * Checks if a payroll run exists for a given year and month.
     */
    public JsonObject findPayrollRun(int year, int month) throws SQLException {
        String sql = "SELECT pr.payroll_run_id, pr.payroll_year, pr.payroll_month, pr.status, " +
                     "pr.processed_by, pr.approved_by, pr.processed_at, pr.approved_at, " +
                     "CONCAT(p.first_name, ' ', p.last_name) AS processor_name, " +
                     "CONCAT(a.first_name, ' ', a.last_name) AS approver_name " +
                     "FROM payroll_run pr " +
                     "LEFT JOIN employee p ON p.employee_id = pr.processed_by " +
                     "LEFT JOIN employee a ON a.employee_id = pr.approved_by " +
                     "WHERE pr.payroll_year = ? AND pr.payroll_month = ?";

        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setInt(1, year);
            st.setInt(2, month);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    JsonObject o = new JsonObject();
                    o.addProperty("payrollRunId", rs.getLong("payroll_run_id"));
                    o.addProperty("payrollYear", rs.getInt("payroll_year"));
                    o.addProperty("payrollMonth", rs.getInt("payroll_month"));
                    o.addProperty("status", rs.getString("status"));
                    o.addProperty("processedBy", rs.getLong("processed_by"));
                    o.addProperty("processorName", rs.getString("processor_name"));
                    long appBy = rs.getLong("approved_by");
                    if (!rs.wasNull()) {
                        o.addProperty("approvedBy", appBy);
                        o.addProperty("approverName", rs.getString("approver_name"));
                    }
                    if (rs.getTimestamp("processed_at") != null) {
                        o.addProperty("processedAt", rs.getTimestamp("processed_at").toString());
                    }
                    if (rs.getTimestamp("approved_at") != null) {
                        o.addProperty("approvedAt", rs.getTimestamp("approved_at").toString());
                    }
                    return o;
                }
            }
        }
        return null;
    }

    /**
     * Lists all historical payroll runs with aggregate metrics.
     */
    public JsonArray findAllPayrollRuns() throws SQLException {
        String sql = "SELECT pr.payroll_run_id, pr.payroll_year, pr.payroll_month, pr.status, " +
                     "pr.processed_at, pr.approved_at, " +
                     "CONCAT(p.first_name, ' ', p.last_name) AS processor_name, " +
                     "COUNT(ep.employee_payroll_id) AS total_employees, " +
                     "COALESCE(SUM(ep.gross_earnings), 0.00) AS total_gross, " +
                     "COALESCE(SUM(ep.total_deductions), 0.00) AS total_deductions, " +
                     "COALESCE(SUM(ep.net_pay), 0.00) AS total_net_pay " +
                     "FROM payroll_run pr " +
                     "LEFT JOIN employee p ON p.employee_id = pr.processed_by " +
                     "LEFT JOIN employee_payroll ep ON ep.payroll_run_id = pr.payroll_run_id " +
                     "GROUP BY pr.payroll_run_id, pr.payroll_year, pr.payroll_month, pr.status, " +
                     "pr.processed_at, pr.approved_at, p.first_name, p.last_name " +
                     "ORDER BY pr.payroll_year DESC, pr.payroll_month DESC";

        JsonArray arr = new JsonArray();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                JsonObject o = new JsonObject();
                o.addProperty("payrollRunId", rs.getLong("payroll_run_id"));
                o.addProperty("payrollYear", rs.getInt("payroll_year"));
                o.addProperty("payrollMonth", rs.getInt("payroll_month"));
                o.addProperty("status", rs.getString("status"));
                o.addProperty("processorName", rs.getString("processor_name"));
                o.addProperty("totalEmployees", rs.getInt("total_employees"));
                o.addProperty("totalGross", rs.getBigDecimal("total_gross"));
                o.addProperty("totalDeductions", rs.getBigDecimal("total_deductions"));
                o.addProperty("totalNetPay", rs.getBigDecimal("total_net_pay"));
                if (rs.getTimestamp("processed_at") != null) {
                    o.addProperty("processedAt", rs.getTimestamp("processed_at").toString());
                }
                if (rs.getTimestamp("approved_at") != null) {
                    o.addProperty("approvedAt", rs.getTimestamp("approved_at").toString());
                }
                arr.add(o);
            }
        }
        return arr;
    }

    /**
     * Updates payroll run status (e.g., from PROCESSED to APPROVED or PAID).
     */
    public boolean updatePayrollRunStatus(long payrollRunId, String newStatus, Long approverId) throws SQLException {
        String sql = "UPDATE payroll_run SET status = ?, approved_by = ?, approved_at = CURRENT_TIMESTAMP " +
                     "WHERE payroll_run_id = ?";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, newStatus);
            if (approverId != null) st.setLong(2, approverId); else st.setNull(2, Types.BIGINT);
            st.setLong(3, payrollRunId);
            boolean ok = st.executeUpdate() > 0;
            if (ok && "PAID".equalsIgnoreCase(newStatus)) {
                try (PreparedStatement st2 = c.prepareStatement(
                        "UPDATE employee_payroll SET payment_status = 'PAID' WHERE payroll_run_id = ?")) {
                    st2.setLong(1, payrollRunId);
                    st2.executeUpdate();
                }
            }
            return ok;
        }
    }

    /**
     * Retrieves the monthly payroll register for all employees in a specific run.
     */
    public JsonArray findPayrollRegister(long payrollRunId, Long departmentId) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT ep.employee_payroll_id, ep.payroll_run_id, ep.employee_id, " +
            "e.employee_code, e.first_name, e.last_name, d.department_name, des.designation_name, " +
            "ep.working_days, ep.present_days, ep.paid_leave_days, ep.lop_days, " +
            "ep.gross_earnings, ep.lop_deduction, ep.total_deductions, ep.net_pay, ep.payment_status " +
            "FROM employee_payroll ep " +
            "JOIN employee e ON e.employee_id = ep.employee_id " +
            "JOIN department d ON d.department_id = e.department_id " +
            "JOIN designation des ON des.designation_id = e.designation_id " +
            "WHERE ep.payroll_run_id = ? "
        );

        if (departmentId != null && departmentId > 0) {
            sql.append("AND e.department_id = ? ");
        }
        sql.append("ORDER BY e.employee_code ASC");

        JsonArray arr = new JsonArray();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement(sql.toString())) {
            st.setLong(1, payrollRunId);
            if (departmentId != null && departmentId > 0) {
                st.setLong(2, departmentId);
            }
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    JsonObject o = new JsonObject();
                    o.addProperty("employeePayrollId", rs.getLong("employee_payroll_id"));
                    o.addProperty("payrollRunId", rs.getLong("payroll_run_id"));
                    o.addProperty("employeeId", rs.getLong("employee_id"));
                    o.addProperty("employeeCode", rs.getString("employee_code"));
                    o.addProperty("employeeName", rs.getString("first_name") + " " + rs.getString("last_name"));
                    o.addProperty("departmentName", rs.getString("department_name"));
                    o.addProperty("designationName", rs.getString("designation_name"));
                    o.addProperty("workingDays", rs.getBigDecimal("working_days"));
                    o.addProperty("presentDays", rs.getBigDecimal("present_days"));
                    o.addProperty("paidLeaveDays", rs.getBigDecimal("paid_leave_days"));
                    o.addProperty("lopDays", rs.getBigDecimal("lop_days"));
                    o.addProperty("grossEarnings", rs.getBigDecimal("gross_earnings"));
                    o.addProperty("lopDeduction", rs.getBigDecimal("lop_deduction"));
                    o.addProperty("totalDeductions", rs.getBigDecimal("total_deductions"));
                    o.addProperty("netPay", rs.getBigDecimal("net_pay"));
                    o.addProperty("paymentStatus", rs.getString("payment_status"));
                    arr.add(o);
                }
            }
        }
        return arr;
    }

    /**
     * Retrieves individual payslip details including line items (Basic, HRA, PF, etc.).
     */
    public JsonObject findPayslip(long payrollRunId, long employeeId) throws SQLException {
        String summarySql = 
            "SELECT ep.employee_payroll_id, ep.payroll_run_id, ep.employee_id, " +
            "pr.payroll_year, pr.payroll_month, pr.status AS payroll_status, " +
            "e.employee_code, e.first_name, e.last_name, e.email, e.date_of_joining, " +
            "d.department_name, des.designation_name, " +
            "ep.working_days, ep.present_days, ep.paid_leave_days, ep.lop_days, " +
            "ep.gross_earnings, ep.lop_deduction, ep.total_deductions, ep.net_pay, ep.payment_status " +
            "FROM employee_payroll ep " +
            "JOIN payroll_run pr ON pr.payroll_run_id = ep.payroll_run_id " +
            "JOIN employee e ON e.employee_id = ep.employee_id " +
            "JOIN department d ON d.department_id = e.department_id " +
            "JOIN designation des ON des.designation_id = e.designation_id " +
            "WHERE ep.payroll_run_id = ? AND ep.employee_id = ?";

        JsonObject payslip = null;
        long employeePayrollId = -1L;

        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement(summarySql)) {
            st.setLong(1, payrollRunId);
            st.setLong(2, employeeId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    payslip = new JsonObject();
                    employeePayrollId = rs.getLong("employee_payroll_id");
                    payslip.addProperty("employeePayrollId", employeePayrollId);
                    payslip.addProperty("payrollRunId", rs.getLong("payroll_run_id"));
                    payslip.addProperty("employeeId", rs.getLong("employee_id"));
                    payslip.addProperty("payrollYear", rs.getInt("payroll_year"));
                    payslip.addProperty("payrollMonth", rs.getInt("payroll_month"));
                    payslip.addProperty("payrollStatus", rs.getString("payroll_status"));
                    payslip.addProperty("employeeCode", rs.getString("employee_code"));
                    payslip.addProperty("employeeName", rs.getString("first_name") + " " + rs.getString("last_name"));
                    payslip.addProperty("email", rs.getString("email"));
                    payslip.addProperty("dateOfJoining", rs.getDate("date_of_joining") != null ? rs.getDate("date_of_joining").toString() : "");
                    payslip.addProperty("departmentName", rs.getString("department_name"));
                    payslip.addProperty("designationName", rs.getString("designation_name"));
                    payslip.addProperty("workingDays", rs.getBigDecimal("working_days"));
                    payslip.addProperty("presentDays", rs.getBigDecimal("present_days"));
                    payslip.addProperty("paidLeaveDays", rs.getBigDecimal("paid_leave_days"));
                    payslip.addProperty("lopDays", rs.getBigDecimal("lop_days"));
                    payslip.addProperty("grossEarnings", rs.getBigDecimal("gross_earnings"));
                    payslip.addProperty("lopDeduction", rs.getBigDecimal("lop_deduction"));
                    payslip.addProperty("totalDeductions", rs.getBigDecimal("total_deductions"));
                    payslip.addProperty("netPay", rs.getBigDecimal("net_pay"));
                    payslip.addProperty("paymentStatus", rs.getString("payment_status"));
                }
            }

            if (payslip != null && employeePayrollId > 0) {
                String itemsSql = 
                    "SELECT pi.amount, sc.component_code, sc.component_name, sc.component_type " +
                    "FROM payroll_item pi " +
                    "JOIN salary_component sc ON sc.component_id = pi.component_id " +
                    "WHERE pi.employee_payroll_id = ? " +
                    "ORDER BY sc.component_type ASC, sc.component_id ASC";

                JsonArray earnings = new JsonArray();
                JsonArray deductions = new JsonArray();

                try (PreparedStatement itemSt = c.prepareStatement(itemsSql)) {
                    itemSt.setLong(1, employeePayrollId);
                    try (ResultSet itemRs = itemSt.executeQuery()) {
                        while (itemRs.next()) {
                            JsonObject item = new JsonObject();
                            item.addProperty("code", itemRs.getString("component_code"));
                            item.addProperty("name", itemRs.getString("component_name"));
                            item.addProperty("amount", itemRs.getBigDecimal("amount"));
                            String type = itemRs.getString("component_type");
                            item.addProperty("type", type);

                            if ("EARNING".equalsIgnoreCase(type)) {
                                earnings.add(item);
                            } else if ("DEDUCTION".equalsIgnoreCase(type)) {
                                deductions.add(item);
                            }
                        }
                    }
                }

                // Add LOP Deduction to deductions array if greater than 0
                BigDecimal lopDed = payslip.get("lopDeduction").getAsBigDecimal();
                if (lopDed.compareTo(BigDecimal.ZERO) > 0) {
                    JsonObject lopItem = new JsonObject();
                    lopItem.addProperty("code", "LOP_DED");
                    lopItem.addProperty("name", "Loss of Pay (" + payslip.get("lopDays").getAsString() + " days)");
                    lopItem.addProperty("amount", lopDed);
                    lopItem.addProperty("type", "DEDUCTION");
                    deductions.add(lopItem);
                }

                payslip.add("earnings", earnings);
                payslip.add("deductions", deductions);
            }
        }
        return payslip;
    }

    /**
     * Retrieves individual payslip details by employeePayrollId.
     */
    public JsonObject findPayslipById(long employeePayrollId) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement("SELECT payroll_run_id, employee_id FROM employee_payroll WHERE employee_payroll_id = ?")) {
            st.setLong(1, employeePayrollId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return findPayslip(rs.getLong("payroll_run_id"), rs.getLong("employee_id"));
                }
            }
        }
        return null;
    }

    /**
     * Returns all payslips for an employee, ordered newest first.
     */
    public JsonArray findEmployeePayslips(long employeeId) throws SQLException {
        String sql = 
            "SELECT ep.employee_payroll_id, ep.payroll_run_id, pr.payroll_year, pr.payroll_month, " +
            "pr.status AS run_status, ep.gross_earnings, ep.total_deductions, ep.net_pay, ep.payment_status, " +
            "ep.created_at " +
            "FROM employee_payroll ep " +
            "JOIN payroll_run pr ON pr.payroll_run_id = ep.payroll_run_id " +
            "WHERE ep.employee_id = ? " +
            "ORDER BY pr.payroll_year DESC, pr.payroll_month DESC";

        JsonArray arr = new JsonArray();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setLong(1, employeeId);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    JsonObject o = new JsonObject();
                    o.addProperty("employeePayrollId", rs.getLong("employee_payroll_id"));
                    o.addProperty("payrollRunId", rs.getLong("payroll_run_id"));
                    o.addProperty("payrollYear", rs.getInt("payroll_year"));
                    o.addProperty("payrollMonth", rs.getInt("payroll_month"));
                    o.addProperty("status", rs.getString("run_status"));
                    o.addProperty("grossEarnings", rs.getBigDecimal("gross_earnings"));
                    o.addProperty("totalDeductions", rs.getBigDecimal("total_deductions"));
                    o.addProperty("netPay", rs.getBigDecimal("net_pay"));
                    o.addProperty("paymentStatus", rs.getString("payment_status"));
                    if (rs.getTimestamp("created_at") != null) {
                        o.addProperty("createdAt", rs.getTimestamp("created_at").toString());
                    }
                    arr.add(o);
                }
            }
        }
        return arr;
    }

    /**
     * Retrieves all active salary components.
     */
    public List<SalaryComponent> getActiveSalaryComponents() throws SQLException {
        List<SalaryComponent> list = new ArrayList<>();
        String sql = "SELECT * FROM salary_component WHERE is_active = TRUE ORDER BY component_id ASC";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                SalaryComponent sc = new SalaryComponent();
                sc.setComponentId(rs.getLong("component_id"));
                sc.setComponentCode(rs.getString("component_code"));
                sc.setComponentName(rs.getString("component_name"));
                sc.setComponentType(rs.getString("component_type"));
                sc.setCalculationMethod(rs.getString("calculation_method"));
                sc.setTaxable(rs.getBoolean("is_taxable"));
                sc.setPfApplicable(rs.getBoolean("is_pf_applicable"));
                sc.setEsiApplicable(rs.getBoolean("is_esi_applicable"));
                sc.setActive(rs.getBoolean("is_active"));
                list.add(sc);
            }
        }
        return list;
    }

    /**
     * Retrieves all income tax slabs.
     */
    public List<TaxSlab> getTaxSlabs(String regime) throws SQLException {
        List<TaxSlab> list = new ArrayList<>();
        String sql = "SELECT * FROM tax_slab " +
                     (regime != null && !regime.isBlank() ? "WHERE tax_regime = ? " : "") +
                     "ORDER BY tax_regime ASC, slab_order ASC";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            if (regime != null && !regime.isBlank()) {
                st.setString(1, regime.trim());
            }
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    TaxSlab ts = new TaxSlab();
                    ts.setTaxSlabId(rs.getLong("tax_slab_id"));
                    ts.setTaxRegime(rs.getString("tax_regime"));
                    ts.setFinancialYear(rs.getString("financial_year"));
                    ts.setSlabOrder(rs.getInt("slab_order"));
                    ts.setLowerLimit(rs.getBigDecimal("lower_limit"));
                    BigDecimal upper = rs.getBigDecimal("upper_limit");
                    ts.setUpperLimit(rs.wasNull() ? null : upper);
                    ts.setTaxRate(rs.getBigDecimal("tax_rate"));
                    ts.setCessRate(rs.getBigDecimal("cess_rate"));
                    list.add(ts);
                }
            }
        }
        return list;
    }
}
