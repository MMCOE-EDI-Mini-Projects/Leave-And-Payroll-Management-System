package com.lpms.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lpms.dao.PayrollDAO;
import com.lpms.service.AttendanceService;
import com.lpms.service.PayrollService;
import com.lpms.util.DBUtil;
import com.lpms.util.Json;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Report & Analytics Controller:
 * Provides filtered reporting data and RFC 4180 compliant CSV exports for:
 * 1. Monthly Payroll Registers
 * 2. Organization / Department Leave Summaries
 * 3. Monthly Attendance & Loss-of-Pay Registers
 */
@WebServlet("/api/reports/*")
public class ReportServlet extends HttpServlet {

    private final PayrollService payrollService = new PayrollService();
    private final PayrollDAO payrollDAO = new PayrollDAO();
    private final AttendanceService attendanceService = new AttendanceService();

    private static Long sessionId(HttpServletRequest req) {
        return (Long) req.getSession().getAttribute("employeeId");
    }

    private static String sessionRole(HttpServletRequest req) {
        return (String) req.getSession().getAttribute("role");
    }

    private static boolean isStaff(HttpServletRequest req) {
        String role = sessionRole(req);
        return "HR_ADMIN".equals(role) || "MANAGER".equals(role);
    }

    private static Long parseLong(String s) {
        try { return s == null ? null : Long.valueOf(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    /** Escapes CSV fields and prevents spreadsheet formula injection (=, +, -, @). */
    private static String csvEscape(Object value) {
        if (value == null) return "";
        String str = value.toString();
        if (str.startsWith("=") || str.startsWith("+") || str.startsWith("-") || str.startsWith("@")) {
            str = "'" + str; // Prefix apostrophe to prevent execution in Excel
        }
        if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            str = "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }

    private Long findRunIdByPeriod(Long year, Long month) {
        if (year == null || month == null) return null;
        String sql = "SELECT payroll_run_id FROM payroll_run WHERE payroll_year = ? AND payroll_month = ? ORDER BY payroll_run_id DESC LIMIT 1";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setLong(1, year);
            st.setLong(2, month);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return rs.getLong("payroll_run_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path == null) path = "/";

        if (!isStaff(req)) {
            Json.error(resp, 403, "Access restricted to HR_ADMIN and Managers.");
            return;
        }

        try {
            switch (path) {
                // ---------- 1. PAYROLL REGISTER REPORT ----------
                case "/payroll": {
                    Long runId = parseLong(req.getParameter("runId"));
                    if (runId == null || runId <= 0) {
                        runId = findRunIdByPeriod(parseLong(req.getParameter("year")), parseLong(req.getParameter("month")));
                    }
                    if (runId == null || runId <= 0) {
                        Json.error(resp, 400, "runId or (year and month) parameter is required.");
                        return;
                    }
                    Long deptId = parseLong(req.getParameter("departmentId"));
                    String sortBy = req.getParameter("sortBy");
                    Json.writeArray(resp, 200, payrollService.getPayrollRegister(runId, deptId, sortBy));
                    break;
                }

                case "/payroll/export": {
                    Long runId = parseLong(req.getParameter("runId"));
                    if (runId == null || runId <= 0) {
                        runId = findRunIdByPeriod(parseLong(req.getParameter("year")), parseLong(req.getParameter("month")));
                    }
                    if (runId == null || runId <= 0) {
                        Json.error(resp, 400, "runId or (year and month) parameter is required.");
                        return;
                    }
                    Long deptId = parseLong(req.getParameter("departmentId"));
                    String sortBy = req.getParameter("sortBy");
                    JsonArray records = payrollService.getPayrollRegister(runId, deptId, sortBy);

                    resp.setContentType("text/csv; charset=UTF-8");
                    resp.setHeader("Content-Disposition", "attachment; filename=\"payroll_register_" + runId + ".csv\"");

                    PrintWriter out = resp.getWriter();
                    out.println("Employee ID,Employee Name,Department,Designation,Working Days,Present Days,Paid Leave Days,LOP Days,Gross Salary,LOP Deduction,Total Deductions,Net Salary,Payment Status");

                    for (JsonElement el : records) {
                        JsonObject r = el.getAsJsonObject();
                        out.println(String.join(",",
                            csvEscape(r.get("employeeCode").getAsString()),
                            csvEscape(r.get("employeeName").getAsString()),
                            csvEscape(r.get("departmentName").getAsString()),
                            csvEscape(r.get("designationName").getAsString()),
                            csvEscape(r.get("workingDays").getAsString()),
                            csvEscape(r.get("presentDays").getAsString()),
                            csvEscape(r.get("paidLeaveDays").getAsString()),
                            csvEscape(r.get("lopDays").getAsString()),
                            csvEscape(r.get("grossEarnings").getAsString()),
                            csvEscape(r.get("lopDeduction").getAsString()),
                            csvEscape(r.get("totalDeductions").getAsString()),
                            csvEscape(r.get("netPay").getAsString()),
                            csvEscape(r.get("paymentStatus").getAsString())
                        ));
                    }
                    out.flush();
                    break;
                }

                // ---------- 2. LEAVE SUMMARY REPORT ----------
                case "/leaves": {
                    int year = LocalDate.now().getYear();
                    String yrParam = req.getParameter("year");
                    if (yrParam != null && !yrParam.isBlank()) {
                        try { year = Integer.parseInt(yrParam.trim()); } catch (NumberFormatException ignored) {}
                    }
                    Long deptId = parseLong(req.getParameter("departmentId"));
                    Json.writeArray(resp, 200, getLeaveSummaries(year, deptId));
                    break;
                }

                case "/leaves/export": {
                    int year = LocalDate.now().getYear();
                    String yrParam = req.getParameter("year");
                    if (yrParam != null && !yrParam.isBlank()) {
                        try { year = Integer.parseInt(yrParam.trim()); } catch (NumberFormatException ignored) {}
                    }
                    Long deptId = parseLong(req.getParameter("departmentId"));
                    JsonArray records = getLeaveSummaries(year, deptId);

                    resp.setContentType("text/csv; charset=UTF-8");
                    resp.setHeader("Content-Disposition", "attachment; filename=\"leave_summary_" + year + ".csv\"");

                    PrintWriter out = resp.getWriter();
                    out.println("Employee ID,Employee Name,Department,Leave Type,Annual Entitlement,Accrued,Used,Remaining Balance");

                    for (JsonElement el : records) {
                        JsonObject r = el.getAsJsonObject();
                        out.println(String.join(",",
                            csvEscape(r.get("employeeCode").getAsString()),
                            csvEscape(r.get("employeeName").getAsString()),
                            csvEscape(r.get("departmentName").getAsString()),
                            csvEscape(r.get("leaveName").getAsString()),
                            csvEscape(r.get("openingBalance").getAsString()),
                            csvEscape(r.get("accrued").getAsString()),
                            csvEscape(r.get("used").getAsString()),
                            csvEscape(r.get("closingBalance").getAsString())
                        ));
                    }
                    out.flush();
                    break;
                }

                // ---------- 3. ATTENDANCE & LOP REPORT ----------
                case "/attendance": {
                    YearMonth month = YearMonth.now();
                    String mParam = req.getParameter("month");
                    if (mParam != null && !mParam.isBlank()) {
                        try { month = YearMonth.parse(mParam.trim()); } catch (Exception ignored) {}
                    }
                    Long deptId = parseLong(req.getParameter("departmentId"));
                    Json.writeArray(resp, 200, getAttendanceSummaries(month, deptId));
                    break;
                }

                case "/attendance/export": {
                    YearMonth month = YearMonth.now();
                    String mParam = req.getParameter("month");
                    if (mParam != null && !mParam.isBlank()) {
                        try { month = YearMonth.parse(mParam.trim()); } catch (Exception ignored) {}
                    }
                    Long deptId = parseLong(req.getParameter("departmentId"));
                    JsonArray records = getAttendanceSummaries(month, deptId);

                    resp.setContentType("text/csv; charset=UTF-8");
                    resp.setHeader("Content-Disposition", "attachment; filename=\"attendance_report_" + month + ".csv\"");

                    PrintWriter out = resp.getWriter();
                    out.println("Employee ID,Employee Name,Department,Month,Working Days,Present Days,Absent Days,Leave Days,LOP Days,Worked Hours,Overtime Hours");

                    for (JsonElement el : records) {
                        JsonObject r = el.getAsJsonObject();
                        out.println(String.join(",",
                            csvEscape(r.get("employeeCode").getAsString()),
                            csvEscape(r.get("employeeName").getAsString()),
                            csvEscape(r.get("departmentName").getAsString()),
                            csvEscape(month.toString()),
                            csvEscape(r.get("workingDays").getAsString()),
                            csvEscape(r.get("presentDays").getAsString()),
                            csvEscape(r.get("absentDays").getAsString()),
                            csvEscape(r.get("leaveDays").getAsString()),
                            csvEscape(r.get("lopDays").getAsString()),
                            csvEscape(r.get("workedHours").getAsString()),
                            csvEscape(r.get("overtimeHours").getAsString())
                        ));
                    }
                    out.flush();
                    break;
                }

                default:
                    Json.error(resp, 404, "Unknown report endpoint: " + path);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Json.error(resp, 500, "Database error generating report: " + e.getMessage());
        }
    }

    /**
     * Queries leave balance summaries for an entire organization or specific department.
     */
    private JsonArray getLeaveSummaries(int year, Long departmentId) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT e.employee_id, e.employee_code, e.first_name, e.last_name, d.department_name, " +
            "lt.leave_name, lb.opening_balance, lb.accrued, lb.used, lb.closing_balance " +
            "FROM employee e " +
            "JOIN department d ON d.department_id = e.department_id " +
            "JOIN leave_balance lb ON lb.employee_id = e.employee_id AND lb.leave_year = ? " +
            "JOIN leave_type lt ON lt.leave_type_id = lb.leave_type_id " +
            "WHERE e.is_active = TRUE "
        );
        if (departmentId != null && departmentId > 0) {
            sql.append("AND e.department_id = ? ");
        }
        sql.append("ORDER BY e.employee_code ASC, lt.leave_name ASC");

        JsonArray arr = new JsonArray();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement(sql.toString())) {
            st.setInt(1, year);
            if (departmentId != null && departmentId > 0) {
                st.setLong(2, departmentId);
            }
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    JsonObject o = new JsonObject();
                    o.addProperty("employeeId", rs.getLong("employee_id"));
                    o.addProperty("employeeCode", rs.getString("employee_code"));
                    o.addProperty("employeeName", rs.getString("first_name") + " " + rs.getString("last_name"));
                    o.addProperty("departmentName", rs.getString("department_name"));
                    o.addProperty("leaveName", rs.getString("leave_name"));
                    o.addProperty("openingBalance", rs.getBigDecimal("opening_balance"));
                    o.addProperty("accrued", rs.getBigDecimal("accrued"));
                    o.addProperty("used", rs.getBigDecimal("used"));
                    o.addProperty("closingBalance", rs.getBigDecimal("closing_balance"));
                    arr.add(o);
                }
            }
        }
        return arr;
    }

    /**
     * Generates monthly attendance & LOP summary report for an organization or department.
     */
    private JsonArray getAttendanceSummaries(YearMonth month, Long departmentId) throws SQLException {
        JsonArray teamRaw = attendanceService.summaries(month, null, null, false);
        JsonArray filtered = new JsonArray();

        // Get department lookup if filtered
        for (JsonElement el : teamRaw) {
            JsonObject o = el.getAsJsonObject();
            long empId = o.get("employeeId").getAsLong();

            // Fetch employee's department info
            String deptName = "General";
            long deptId = 0L;
            try (Connection c = DBUtil.getConnection();
                 PreparedStatement st = c.prepareStatement(
                     "SELECT d.department_id, d.department_name FROM employee e " +
                     "JOIN department d ON d.department_id = e.department_id WHERE e.employee_id = ?")) {
                st.setLong(1, empId);
                try (ResultSet rs = st.executeQuery()) {
                    if (rs.next()) {
                        deptId = rs.getLong("department_id");
                        deptName = rs.getString("department_name");
                    }
                }
            }

            if (departmentId != null && departmentId > 0 && departmentId != deptId) {
                continue;
            }

            JsonObject item = new JsonObject();
            item.addProperty("employeeId", empId);
            item.addProperty("employeeCode", o.get("employeeCode").getAsString());
            item.addProperty("employeeName", o.get("employeeName").getAsString());
            item.addProperty("departmentName", deptName);
            item.addProperty("workingDays", o.get("workingDays").getAsBigDecimal());
            item.addProperty("presentDays", o.get("presentDays").getAsBigDecimal());
            item.addProperty("absentDays", o.get("absentDays").getAsBigDecimal());
            item.addProperty("leaveDays", o.get("approvedLeaveDays").getAsBigDecimal());
            item.addProperty("lopDays", o.get("lopDays").getAsBigDecimal());

            int workedMins = o.has("workedMinutes") ? o.get("workedMinutes").getAsInt() : 0;
            int otMins = o.has("overtimeMinutes") ? o.get("overtimeMinutes").getAsInt() : 0;

            BigDecimal workedHours = new BigDecimal(workedMins).divide(new BigDecimal("60"), 1, java.math.RoundingMode.HALF_UP);
            BigDecimal otHours = new BigDecimal(otMins).divide(new BigDecimal("60"), 1, java.math.RoundingMode.HALF_UP);

            item.addProperty("workedHours", workedHours);
            item.addProperty("overtimeHours", otHours);
            filtered.add(item);
        }
        return filtered;
    }
}
