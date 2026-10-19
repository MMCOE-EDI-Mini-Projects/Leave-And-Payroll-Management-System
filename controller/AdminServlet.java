package com.lpms.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lpms.dao.AdminConfigDAO;
import com.lpms.dao.AuditDAO;
import com.lpms.model.EmployeeSalaryDetail;
import com.lpms.util.Json;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Module 7: Administration & Master Data Configuration.
 * Restricted to HR_ADMIN users.
 */
@WebServlet("/api/admin/*")
public class AdminServlet extends HttpServlet {

    private final AdminConfigDAO dao = new AdminConfigDAO();
    private final AuditDAO auditDAO = new AuditDAO();

    private static boolean isHr(HttpServletRequest req) {
        return "HR_ADMIN".equals(req.getSession().getAttribute("role"));
    }

    private static Long sessionId(HttpServletRequest req) {
        return (Long) req.getSession().getAttribute("employeeId");
    }

    private static Long parseLong(String s) {
        try { return s == null ? null : Long.valueOf(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private static BigDecimal parseBigDecimal(String s, BigDecimal fallback) {
        try { return s == null ? fallback : new BigDecimal(s.trim()); } catch (Exception e) { return fallback; }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!isHr(req)) {
            Json.error(resp, 403, "Access restricted to HR_ADMIN.");
            return;
        }

        String path = req.getPathInfo();
        if (path == null) path = "/";

        try {
            switch (path) {
                case "/departments":
                    Json.writeArray(resp, 200, dao.listDepartments());
                    break;

                case "/designations":
                    Json.writeArray(resp, 200, dao.listDesignations());
                    break;

                case "/leave-types":
                    Json.writeArray(resp, 200, dao.listLeaveTypes());
                    break;

                case "/salary-components":
                    Json.writeArray(resp, 200, dao.listSalaryComponents());
                    break;

                case "/tax-slabs":
                    Json.writeArray(resp, 200, dao.listTaxSlabs());
                    break;

                case "/employee-salary": {
                    Long empId = parseLong(req.getParameter("employeeId"));
                    if (empId == null || empId <= 0) {
                        Json.error(resp, 400, "employeeId is required.");
                        return;
                    }
                    JsonObject sal = dao.getEmployeeSalary(empId);
                    if (sal == null) {
                        Json.write(resp, 200, new JsonObject());
                    } else {
                        Json.write(resp, 200, sal);
                    }
                    break;
                }

                case "/audit": {
                    int limit = 50;
                    String l = req.getParameter("limit");
                    if (l != null && !l.isBlank()) {
                        try { limit = Integer.parseInt(l.trim()); } catch (NumberFormatException ignored) {}
                    }
                    String entity = req.getParameter("entity");
                    Json.writeArray(resp, 200, auditDAO.findRecent(limit, entity));
                    break;
                }

                default:
                    Json.error(resp, 404, "Unknown admin endpoint: " + path);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Json.error(resp, 500, "Database error: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!isHr(req)) {
            Json.error(resp, 403, "Access restricted to HR_ADMIN.");
            return;
        }

        String path = req.getPathInfo();
        if (path == null) path = "/";

        JsonObject body = Json.readBody(req);
        if (body == null) {
            Json.error(resp, 400, "Invalid JSON body.");
            return;
        }

        long me = sessionId(req);

        try {
            switch (path) {
                // ---------- DEPARTMENTS ----------
                case "/departments": {
                    String code = Json.str(body, "departmentCode");
                    String name = Json.str(body, "departmentName");
                    String desc = Json.str(body, "description");
                    if (code == null || name == null) {
                        Json.error(resp, 400, "departmentCode and departmentName are required.");
                        return;
                    }
                    long id = dao.createDepartment(code, name, desc);
                    auditDAO.log("department", id, "INSERT", me, null, "{\"name\":\"" + name + "\"}");
                    Json.message(resp, 201, "Department created with ID: " + id);
                    break;
                }

                case "/departments/toggle": {
                    Long id = body.has("id") ? body.get("id").getAsLong() : null;
                    Boolean active = body.has("active") ? body.get("active").getAsBoolean() : null;
                    if (id == null || active == null) {
                        Json.error(resp, 400, "id and active boolean are required.");
                        return;
                    }
                    dao.toggleDepartmentStatus(id, active);
                    auditDAO.log("department", id, "UPDATE", me, null, "{\"active\":" + active + "}");
                    Json.message(resp, 200, "Department status updated.");
                    break;
                }

                // ---------- DESIGNATIONS ----------
                case "/designations": {
                    String code = Json.str(body, "designationCode");
                    String name = Json.str(body, "designationName");
                    String level = Json.str(body, "jobLevel");
                    String desc = Json.str(body, "description");
                    if (code == null || name == null) {
                        Json.error(resp, 400, "designationCode and designationName are required.");
                        return;
                    }
                    long id = dao.createDesignation(code, name, level, desc);
                    auditDAO.log("designation", id, "INSERT", me, null, "{\"name\":\"" + name + "\"}");
                    Json.message(resp, 201, "Designation created with ID: " + id);
                    break;
                }

                case "/designations/toggle": {
                    Long id = body.has("id") ? body.get("id").getAsLong() : null;
                    Boolean active = body.has("active") ? body.get("active").getAsBoolean() : null;
                    if (id == null || active == null) {
                        Json.error(resp, 400, "id and active boolean are required.");
                        return;
                    }
                    dao.toggleDesignationStatus(id, active);
                    auditDAO.log("designation", id, "UPDATE", me, null, "{\"active\":" + active + "}");
                    Json.message(resp, 200, "Designation status updated.");
                    break;
                }

                // ---------- LEAVE TYPES ----------
                case "/leave-types": {
                    String code = Json.str(body, "leaveCode");
                    String name = Json.str(body, "leaveName");
                    String desc = Json.str(body, "description");
                    BigDecimal entitlement = parseBigDecimal(Json.str(body, "annualEntitlement"), new BigDecimal("12.00"));
                    boolean isPaid = !"false".equalsIgnoreCase(Json.str(body, "isPaid"));
                    boolean halfDay = !"false".equalsIgnoreCase(Json.str(body, "allowsHalfDay"));
                    boolean carry = "true".equalsIgnoreCase(Json.str(body, "allowsCarryForward"));
                    BigDecimal maxCarry = parseBigDecimal(Json.str(body, "maxCarryForward"), BigDecimal.ZERO);

                    if (code == null || name == null) {
                        Json.error(resp, 400, "leaveCode and leaveName are required.");
                        return;
                    }
                    long id = dao.createLeaveType(code, name, desc, entitlement, isPaid, halfDay, carry, maxCarry);
                    auditDAO.log("leave_type", id, "INSERT", me, null, "{\"name\":\"" + name + "\"}");
                    Json.message(resp, 201, "Leave type created with ID: " + id);
                    break;
                }

                // ---------- SALARY COMPONENTS ----------
                case "/salary-components": {
                    String code = Json.str(body, "componentCode");
                    String name = Json.str(body, "componentName");
                    String type = Json.str(body, "componentType");
                    String method = Json.str(body, "calculationMethod");
                    boolean taxable = !"false".equalsIgnoreCase(Json.str(body, "isTaxable"));
                    boolean pf = "true".equalsIgnoreCase(Json.str(body, "isPfApplicable"));

                    if (code == null || name == null || type == null || method == null) {
                        Json.error(resp, 400, "componentCode, componentName, componentType, and calculationMethod are required.");
                        return;
                    }
                    long id = dao.createSalaryComponent(code, name, type, method, taxable, pf);
                    auditDAO.log("salary_component", id, "INSERT", me, null, "{\"name\":\"" + name + "\"}");
                    Json.message(resp, 201, "Salary component created with ID: " + id);
                    break;
                }

                // ---------- TAX SLABS ----------
                case "/tax-slabs": {
                    String regime = Json.str(body, "taxRegime");
                    String finYear = Json.str(body, "financialYear");
                    int order = body.has("slabOrder") ? body.get("slabOrder").getAsInt() : 1;
                    BigDecimal lower = parseBigDecimal(Json.str(body, "lowerLimit"), BigDecimal.ZERO);
                    BigDecimal upper = body.has("upperLimit") && !body.get("upperLimit").isJsonNull() ? 
                                       parseBigDecimal(body.get("upperLimit").getAsString(), null) : null;
                    BigDecimal rate = parseBigDecimal(Json.str(body, "taxRate"), BigDecimal.ZERO);
                    BigDecimal cess = parseBigDecimal(Json.str(body, "cessRate"), new BigDecimal("4.00"));

                    if (regime == null || finYear == null) {
                        Json.error(resp, 400, "taxRegime and financialYear are required.");
                        return;
                    }
                    long id = dao.saveTaxSlab(regime, finYear, order, lower, upper, rate, cess);
                    auditDAO.log("tax_slab", id, "UPDATE", me, null, "{\"regime\":\"" + regime + "\",\"order\":" + order + "}");
                    Json.message(resp, 200, "Tax slab configured successfully.");
                    break;
                }

                // ---------- EMPLOYEE SALARY MASTER ----------
                case "/employee-salary": {
                    Long empId = body.has("employeeId") ? body.get("employeeId").getAsLong() : null;
                    BigDecimal ctc = parseBigDecimal(Json.str(body, "annualCtc"), null);
                    BigDecimal gross = parseBigDecimal(Json.str(body, "monthlyGross"), null);

                    if (empId == null || ctc == null || gross == null) {
                        Json.error(resp, 400, "employeeId, annualCtc, and monthlyGross are required.");
                        return;
                    }

                    List<EmployeeSalaryDetail> details = new ArrayList<>();
                    if (body.has("components") && body.get("components").isJsonArray()) {
                        for (JsonElement el : body.getAsJsonArray("components")) {
                            JsonObject item = el.getAsJsonObject();
                            EmployeeSalaryDetail d = new EmployeeSalaryDetail();
                            d.setComponentId(item.get("componentId").getAsLong());
                            d.setComponentValue(item.get("componentValue").getAsBigDecimal());
                            details.add(d);
                        }
                    }

                    dao.saveEmployeeSalary(empId, ctc, gross, details);
                    auditDAO.log("employee_salary", empId, "UPDATE", me, null, "{\"annualCtc\":" + ctc + ",\"monthlyGross\":" + gross + "}");
                    Json.message(resp, 200, "Employee salary structure saved successfully.");
                    break;
                }

                default:
                    Json.error(resp, 404, "Unknown admin POST endpoint: " + path);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Json.error(resp, 500, "Database error: " + e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!isHr(req)) {
            Json.error(resp, 403, "Access restricted to HR_ADMIN.");
            return;
        }

        String path = req.getPathInfo();
        if (path == null) path = "/";

        JsonObject body = Json.readBody(req);
        if (body == null) {
            Json.error(resp, 400, "Invalid JSON body.");
            return;
        }

        long me = sessionId(req);

        try {
            switch (path) {
                case "/departments": {
                    Long id = body.has("departmentId") ? body.get("departmentId").getAsLong() : null;
                    String name = Json.str(body, "departmentName");
                    String desc = Json.str(body, "description");
                    if (id == null || name == null) {
                        Json.error(resp, 400, "departmentId and departmentName are required.");
                        return;
                    }
                    dao.updateDepartment(id, name, desc);
                    auditDAO.log("department", id, "UPDATE", me, null, "{\"name\":\"" + name + "\"}");
                    Json.message(resp, 200, "Department updated successfully.");
                    break;
                }

                case "/designations": {
                    Long id = body.has("designationId") ? body.get("designationId").getAsLong() : null;
                    String name = Json.str(body, "designationName");
                    String level = Json.str(body, "jobLevel");
                    String desc = Json.str(body, "description");
                    if (id == null || name == null) {
                        Json.error(resp, 400, "designationId and designationName are required.");
                        return;
                    }
                    dao.updateDesignation(id, name, level, desc);
                    auditDAO.log("designation", id, "UPDATE", me, null, "{\"name\":\"" + name + "\"}");
                    Json.message(resp, 200, "Designation updated successfully.");
                    break;
                }

                case "/leave-types": {
                    Long id = body.has("leaveTypeId") ? body.get("leaveTypeId").getAsLong() : null;
                    String name = Json.str(body, "leaveName");
                    BigDecimal entitlement = parseBigDecimal(Json.str(body, "annualEntitlement"), new BigDecimal("12.00"));
                    boolean isPaid = !"false".equalsIgnoreCase(Json.str(body, "isPaid"));
                    boolean halfDay = !"false".equalsIgnoreCase(Json.str(body, "allowsHalfDay"));
                    boolean carry = "true".equalsIgnoreCase(Json.str(body, "allowsCarryForward"));
                    BigDecimal maxCarry = parseBigDecimal(Json.str(body, "maxCarryForward"), BigDecimal.ZERO);
                    boolean active = !"false".equalsIgnoreCase(Json.str(body, "isActive"));

                    if (id == null || name == null) {
                        Json.error(resp, 400, "leaveTypeId and leaveName are required.");
                        return;
                    }
                    dao.updateLeaveType(id, name, entitlement, isPaid, halfDay, carry, maxCarry, active);
                    auditDAO.log("leave_type", id, "UPDATE", me, null, "{\"name\":\"" + name + "\"}");
                    Json.message(resp, 200, "Leave type updated successfully.");
                    break;
                }

                case "/salary-components": {
                    Long id = body.has("componentId") ? body.get("componentId").getAsLong() : null;
                    String name = Json.str(body, "componentName");
                    String type = Json.str(body, "componentType");
                    String method = Json.str(body, "calculationMethod");
                    boolean taxable = !"false".equalsIgnoreCase(Json.str(body, "isTaxable"));
                    boolean pf = "true".equalsIgnoreCase(Json.str(body, "isPfApplicable"));
                    boolean active = !"false".equalsIgnoreCase(Json.str(body, "isActive"));

                    if (id == null || name == null || type == null || method == null) {
                        Json.error(resp, 400, "componentId, componentName, componentType, and calculationMethod are required.");
                        return;
                    }
                    dao.updateSalaryComponent(id, name, type, method, taxable, pf, active);
                    auditDAO.log("salary_component", id, "UPDATE", me, null, "{\"name\":\"" + name + "\"}");
                    Json.message(resp, 200, "Salary component updated successfully.");
                    break;
                }

                default:
                    Json.error(resp, 404, "Unknown admin PUT endpoint: " + path);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Json.error(resp, 500, "Database error: " + e.getMessage());
        }
    }
}
