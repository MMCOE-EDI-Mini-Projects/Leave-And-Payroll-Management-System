package com.lpms.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lpms.service.PayrollException;
import com.lpms.service.PayrollService;
import com.lpms.util.Json;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * REST Servlet Controller for Payroll Processing and Payslip access.
 * 
 * Endpoints:
 * GET  /api/payroll/runs                     (HR_ADMIN) List all historical payroll runs
 * GET  /api/payroll/register?runId=&dept=&sortBy= (HR_ADMIN) Full payroll register sorted with MergeSort
 * GET  /api/payroll/payslip?runId=&empId=    (HR_ADMIN or self) Individual detailed payslip
 * GET  /api/payroll/my-payslips              (All authenticated employees) Historical payslip list
 * POST /api/payroll/run                      (HR_ADMIN) Execute transactional payroll run
 * POST /api/payroll/status                   (HR_ADMIN) Update status (APPROVE / PAID)
 */
@WebServlet("/api/payroll/*")
public class PayrollServlet extends HttpServlet {

    private final PayrollService service = new PayrollService();

    private static Long sessionId(HttpServletRequest req) {
        return (Long) req.getSession().getAttribute("employeeId");
    }

    private static String sessionRole(HttpServletRequest req) {
        return (String) req.getSession().getAttribute("role");
    }

    private static boolean isHr(HttpServletRequest req) {
        return "HR_ADMIN".equals(sessionRole(req));
    }

    private static Long parseLong(String s) {
        try { return s == null ? null : Long.valueOf(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path == null) path = "/";

        long me = sessionId(req);
        String role = sessionRole(req);

        try {
            switch (path) {
                case "/runs": {
                    if (!isHr(req)) {
                        Json.error(resp, 403, "Only HR_ADMIN can view all payroll runs.");
                        return;
                    }
                    Json.writeArray(resp, 200, service.getPayrollRuns());
                    break;
                }

                case "/register": {
                    if (!isHr(req) && !"MANAGER".equals(role)) {
                        Json.error(resp, 403, "Insufficient permissions to view payroll register.");
                        return;
                    }
                    Long runId = parseLong(req.getParameter("runId"));
                    if (runId == null || runId <= 0) {
                        Json.error(resp, 400, "runId parameter is required.");
                        return;
                    }
                    Long deptId = parseLong(req.getParameter("departmentId"));
                    String sortBy = req.getParameter("sortBy");
                    Json.writeArray(resp, 200, service.getPayrollRegister(runId, deptId, sortBy));
                    break;
                }

                case "/payslip": {
                    Long payrollId = parseLong(req.getParameter("payrollId"));
                    Long runId = parseLong(req.getParameter("runId"));
                    Long empId = parseLong(req.getParameter("empId"));

                    JsonObject slip = null;
                    if (payrollId != null && payrollId > 0) {
                        slip = service.getPayslipById(payrollId);
                    } else if (runId != null && runId > 0) {
                        if (empId == null || empId <= 0) {
                            empId = me;
                        }
                        slip = service.getPayslip(runId, empId);
                    } else {
                        Json.error(resp, 400, "payrollId or runId is required.");
                        return;
                    }

                    if (slip == null) {
                        Json.error(resp, 404, "Payslip not found.");
                        return;
                    }

                    long targetEmpId = slip.get("employeeId").getAsLong();
                    // Security check: only HR or the employee themselves may view this payslip
                    if (!isHr(req) && targetEmpId != me) {
                        Json.error(resp, 403, "You can only view your own payslip.");
                        return;
                    }

                    Json.write(resp, 200, slip);
                    break;
                }

                case "/my-payslips": {
                    Json.writeArray(resp, 200, service.getMyPayslips(me));
                    break;
                }

                default:
                    Json.error(resp, 404, "Unknown payroll endpoint: " + path);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Json.error(resp, 500, "Database error retrieving payroll data: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path == null) path = "/";

        if (!isHr(req)) {
            Json.error(resp, 403, "Only HR_ADMIN can perform payroll operations.");
            return;
        }

        JsonObject body = Json.readBody(req);
        if (body == null) {
            Json.error(resp, 400, "Invalid JSON body.");
            return;
        }

        long me = sessionId(req);

        try {
            switch (path) {
                case "/run": {
                    if (!body.has("payrollYear") || !body.has("payrollMonth")) {
                        Json.error(resp, 400, "payrollYear and payrollMonth are required.");
                        return;
                    }
                    int year = body.get("payrollYear").getAsInt();
                    int month = body.get("payrollMonth").getAsInt();

                    JsonObject summary = service.processPayroll(year, month, me);
                    Json.write(resp, 200, summary);
                    break;
                }

                case "/status":
                case "/approve": {
                    if (!body.has("payrollRunId") || !body.has("status")) {
                        Json.error(resp, 400, "payrollRunId and status (APPROVED or PAID) are required.");
                        return;
                    }
                    long runId = body.get("payrollRunId").getAsLong();
                    String status = body.get("status").getAsString();

                    boolean updated = service.updateStatus(runId, status, me);
                    if (updated) {
                        Json.message(resp, 200, "Payroll run #" + runId + " marked as " + status.toUpperCase());
                    } else {
                        Json.error(resp, 404, "Payroll run not found.");
                    }
                    break;
                }

                default:
                    Json.error(resp, 404, "Unknown payroll POST endpoint: " + path);
            }
        } catch (PayrollException e) {
            Json.error(resp, e.getStatus(), e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            Json.error(resp, 500, "Database error processing payroll: " + e.getMessage());
        }
    }
}
