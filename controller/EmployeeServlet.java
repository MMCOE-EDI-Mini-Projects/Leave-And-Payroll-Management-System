package com.lpms.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.lpms.dao.AuditDAO;
import com.lpms.dao.EmployeeDAO;
import com.lpms.model.Employee;
import com.lpms.model.Role;
import com.lpms.service.AuthService;
import com.lpms.util.EmployeeValidator;
import com.lpms.util.Json;
import com.lpms.util.PasswordPolicy;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * Employee management. HR_ADMIN manages; MANAGER sees direct reports; EMPLOYEE sees only self.
 *   GET    /api/employees?q=&departmentId=&status=&active=   HR: all (filtered), Manager: direct reports (filtered), Employee: 403
 *   GET    /api/employees/{id}                self, HR, or the employee's manager
 *   POST   /api/employees                     HR: create
 *   PUT    /api/employees/{id}                HR: update profile/assignment/status (code, username, role, password not changed here)
 *   DELETE /api/employees/{id}                HR: permanent delete, only if the employee has no history (else deactivate)
 *   POST   /api/employees/{id}/reset-password HR
 *   PUT    /api/employees/{id}/status         HR: {active:boolean, employmentStatus?:RESIGNED|TERMINATED}
 *   PUT    /api/employees/{id}/role           HR: {role}
 */
@WebServlet("/api/employees/*")
public class EmployeeServlet extends HttpServlet {

    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final AuditDAO auditDAO = new AuditDAO();
    private final AuthService authService = new AuthService();
    private final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd").create();

    private static Long sessionId(HttpServletRequest req) { return (Long) req.getSession().getAttribute("employeeId"); }
    private static String sessionRole(HttpServletRequest req) { return (String) req.getSession().getAttribute("role"); }
    private static boolean isHr(HttpServletRequest req) { return "HR_ADMIN".equals(sessionRole(req)); }

    /** @return the numeric path id, or -1 if malformed. */
    private static Long pathId(String segment) {
        try { return Long.parseLong(segment); } catch (NumberFormatException e) { return -1L; }
    }

    private static Date toDate(LocalDate d) {
        return d == null ? null : Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private boolean requireHr(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (isHr(req)) return true;
        Json.error(resp, 403, "Only HR_ADMIN can manage employee accounts.");
        return false;
    }

    // ------------------------------------------------------------------ GET

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        String role = sessionRole(req);
        String path = req.getPathInfo();

        if (path == null || path.equals("/")) { list(req, resp, role); return; }

        String[] parts = path.substring(1).split("/");
        Long id = pathId(parts[0]);
        if (parts.length != 1 || id < 0) { Json.error(resp, 400, "Invalid employee ID"); return; }
        Employee target = employeeDAO.findDetailById(id);
        if (target == null) { Json.error(resp, 404, "Employee not found"); return; }

        boolean self = id.equals(sessionId(req));
        boolean theirManager = "MANAGER".equals(role) && sessionId(req).equals(target.getReportingManagerId());
        if (!(self || isHr(req) || theirManager)) { Json.error(resp, 403, "Forbidden: insufficient role."); return; }
        resp.getWriter().write(gson.toJson(target));
    }

    private void list(HttpServletRequest req, HttpServletResponse resp, String role) throws IOException {
        Long managerScope = null;
        if ("MANAGER".equals(role)) managerScope = sessionId(req);
        else if (!"HR_ADMIN".equals(role)) { Json.error(resp, 403, "Forbidden: insufficient role."); return; }

        String q = req.getParameter("q");
        if (q != null && q.length() > 100) { Json.error(resp, 400, "Search text is too long."); return; }

        Long dept = null;
        String deptParam = req.getParameter("departmentId");
        if (deptParam != null && !deptParam.isBlank()) {
            dept = pathId(deptParam.trim());
            if (dept < 0) { Json.error(resp, 400, "Invalid department filter."); return; }
        }
        String status = req.getParameter("status");
        if (status != null && status.isBlank()) status = null;
        if (status != null && EmployeeValidator.status(status) != null) { Json.error(resp, 400, "Invalid status filter."); return; }

        Boolean active = null;
        String activeParam = req.getParameter("active");
        if ("true".equals(activeParam)) active = Boolean.TRUE;
        else if ("false".equals(activeParam)) active = Boolean.FALSE;
        else if (activeParam != null && !activeParam.isBlank()) { Json.error(resp, 400, "Invalid active filter."); return; }

        try {
            List<Employee> list = employeeDAO.search(q, dept, status, active, managerScope);
            resp.getWriter().write(gson.toJson(list));
        } catch (SQLException e) {
            e.printStackTrace();
            Json.error(resp, 500, "Failed to load employees.");
        }
    }

    // ------------------------------------------------------------------ POST

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!requireHr(req, resp)) return;
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) { createEmployee(req, resp); return; }

        String[] parts = path.substring(1).split("/");
        if (parts.length == 2 && parts[1].equals("reset-password")) { resetPassword(req, resp, pathId(parts[0])); return; }
        Json.error(resp, 404, "Not found");
    }

    // ------------------------------------------------------------------ PUT

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!requireHr(req, resp)) return;
        String path = req.getPathInfo();
        String[] parts = (path == null || path.equals("/")) ? new String[0] : path.substring(1).split("/");
        if (parts.length < 1 || parts.length > 2) { Json.error(resp, 404, "Not found"); return; }
        Long id = pathId(parts[0]);
        if (id < 0) { Json.error(resp, 400, "Invalid employee ID"); return; }
        Employee target = employeeDAO.findById(id);
        if (target == null) { Json.error(resp, 404, "Employee not found"); return; }
        JsonObject body = Json.readBody(req);
        if (body == null) { Json.error(resp, 400, "Invalid JSON body"); return; }

        if (parts.length == 1) { updateEmployee(req, resp, target, body); return; }

        // Lockout protection: HR cannot change their own role/status.
        if (id.equals(sessionId(req))) { Json.error(resp, 400, "You cannot change your own role or status."); return; }

        if (parts[1].equals("status")) {
            if (!body.has("active") || !body.get("active").isJsonPrimitive()) { Json.error(resp, 400, "'active' is required"); return; }
            boolean active = body.get("active").getAsBoolean();
            String exitStatus = Json.str(body, "employmentStatus");
            if (exitStatus == null) exitStatus = "RESIGNED";
            if (!EmployeeValidator.EXITED.contains(exitStatus)) { Json.error(resp, 400, "Deactivation status must be RESIGNED or TERMINATED."); return; }
            if (!active && target.getRole() == Role.HR_ADMIN && target.isActive() && employeeDAO.countActiveHrAdmins() <= 1) {
                Json.error(resp, 400, "Cannot deactivate the last active HR_ADMIN."); return;
            }
            if (employeeDAO.setAccountState(id, active, exitStatus)) {
                auditDAO.log("employee", id, "UPDATE", sessionId(req), gson.toJson(target), gson.toJson(employeeDAO.findById(id)));
                Json.message(resp, 200, active ? "Account activated." : "Account deactivated.");
            } else Json.error(resp, 500, "Update failed.");
        } else if (parts[1].equals("role")) {
            Role newRole;
            try { newRole = Role.valueOf(String.valueOf(Json.str(body, "role"))); }
            catch (IllegalArgumentException e) { Json.error(resp, 400, "Invalid role."); return; }
            if (target.getRole() == Role.HR_ADMIN && newRole != Role.HR_ADMIN && target.isActive() && employeeDAO.countActiveHrAdmins() <= 1) {
                Json.error(resp, 400, "Cannot demote the last active HR_ADMIN."); return;
            }
            if (employeeDAO.updateRole(id, newRole)) {
                auditDAO.log("employee", id, "UPDATE", sessionId(req), gson.toJson(target), gson.toJson(employeeDAO.findById(id)));
                Json.message(resp, 200, "Role updated.");
            } else Json.error(resp, 500, "Update failed.");
        } else {
            Json.error(resp, 404, "Not found");
        }
    }

    private void updateEmployee(HttpServletRequest req, HttpServletResponse resp, Employee target, JsonObject j) throws IOException {
        Long id = target.getEmployeeId();
        String first = Json.str(j, "firstName"), last = Json.str(j, "lastName"), gender = Json.str(j, "gender");
        String email = Json.str(j, "email"), phone = Json.str(j, "phone"), type = Json.str(j, "employmentType");
        String status = Json.str(j, "employmentStatus");
        if (first == null || last == null || gender == null || email == null || type == null || status == null
                || Json.str(j, "dateOfJoining") == null || Json.str(j, "departmentId") == null || Json.str(j, "designationId") == null) {
            Json.error(resp, 400, "Invalid data format or missing fields"); return;
        }
        String err = firstError(EmployeeValidator.name(first, "First name"), EmployeeValidator.name(last, "Last name"),
                EmployeeValidator.gender(gender), EmployeeValidator.email(email), EmployeeValidator.phone(phone),
                EmployeeValidator.type(type), EmployeeValidator.status(status));
        if (err != null) { Json.error(resp, 400, err); return; }

        LocalDate joining = EmployeeValidator.parseDate(Json.str(j, "dateOfJoining"));
        LocalDate dob = Json.str(j, "dateOfBirth") == null ? null : EmployeeValidator.parseDate(Json.str(j, "dateOfBirth"));
        LocalDate exit = Json.str(j, "dateOfExit") == null ? null : EmployeeValidator.parseDate(Json.str(j, "dateOfExit"));
        if (joining == null || (Json.str(j, "dateOfBirth") != null && dob == null) || (Json.str(j, "dateOfExit") != null && exit == null)) {
            Json.error(resp, 400, "Dates must be valid and formatted yyyy-MM-dd."); return;
        }

        boolean exited = EmployeeValidator.EXITED.contains(status);
        if (exited && exit == null) exit = LocalDate.now().isBefore(joining) ? joining : LocalDate.now();
        if (!exited) exit = null; // exit date only applies to RESIGNED / TERMINATED
        err = EmployeeValidator.dates(dob, joining, exit, LocalDate.now());
        if (err != null) { Json.error(resp, 400, err); return; }

        if (exited && id.equals(sessionId(req))) { Json.error(resp, 400, "You cannot resign or terminate your own account."); return; }
        if (exited && target.isActive() && target.getRole() == Role.HR_ADMIN && employeeDAO.countActiveHrAdmins() <= 1) {
            Json.error(resp, 400, "Cannot deactivate the last active HR_ADMIN."); return;
        }

        try {
            Long mgrId = null;
            if (Json.str(j, "reportingManagerId") != null) {
                mgrId = j.get("reportingManagerId").getAsLong();
                if (mgrId.equals(id)) { Json.error(resp, 400, "An employee cannot report to themselves."); return; }
                Employee mgr = employeeDAO.findById(mgrId);
                if (mgr == null || !mgr.isActive() || mgr.getRole() == Role.EMPLOYEE) {
                    Json.error(resp, 400, "Reporting manager must be an active MANAGER or HR_ADMIN."); return;
                }
                if (employeeDAO.wouldCreateCycle(id, mgrId)) { Json.error(resp, 400, "That reporting manager would create a reporting loop."); return; }
            }
            Employee old = employeeDAO.findById(id);
            Employee u = new Employee();
            u.setEmployeeId(id);
            u.setFirstName(first);
            u.setLastName(last);
            u.setGender(gender);
            u.setDateOfBirth(toDate(dob));
            u.setEmail(email);
            u.setPhone(phone);
            u.setDateOfJoining(toDate(joining));
            u.setDateOfExit(toDate(exit));
            u.setEmploymentType(type);
            u.setEmploymentStatus(status);
            u.setDepartmentId(j.get("departmentId").getAsLong());
            u.setDesignationId(j.get("designationId").getAsLong());
            u.setReportingManagerId(mgrId);
            u.setActive(!exited);

            if (employeeDAO.updateProfile(u)) {
                auditDAO.log("employee", id, "UPDATE", sessionId(req), gson.toJson(old), gson.toJson(employeeDAO.findById(id)));
                Json.message(resp, 200, "Employee updated successfully");
            } else Json.error(resp, 500, "Update failed.");
        } catch (UnsupportedOperationException | IllegalStateException | NumberFormatException e) {
            Json.error(resp, 400, "Invalid data format or missing fields");
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) Json.error(resp, 409, "Email already belongs to another employee.");
            else if (e.getErrorCode() == 1452) Json.error(resp, 400, "Unknown department, designation or manager.");
            else { e.printStackTrace(); Json.error(resp, 500, "Update failed."); }
        }
    }

    // ------------------------------------------------------------------ DELETE

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!requireHr(req, resp)) return;
        String path = req.getPathInfo();
        String[] parts = (path == null || path.equals("/")) ? new String[0] : path.substring(1).split("/");
        if (parts.length != 1) { Json.error(resp, 404, "Not found"); return; }
        Long id = pathId(parts[0]);
        if (id < 0) { Json.error(resp, 400, "Invalid employee ID"); return; }
        Employee target = employeeDAO.findById(id);
        if (target == null) { Json.error(resp, 404, "Employee not found"); return; }
        if (id.equals(sessionId(req))) { Json.error(resp, 400, "You cannot delete your own account."); return; }
        if (target.getRole() == Role.HR_ADMIN && target.isActive() && employeeDAO.countActiveHrAdmins() <= 1) {
            Json.error(resp, 400, "Cannot delete the last active HR_ADMIN."); return;
        }
        try {
            String blocker = employeeDAO.deleteBlocker(id);
            if (blocker != null) {
                Json.error(resp, 409, "Cannot delete: employee has " + blocker + ". Deactivate the account instead to preserve history.");
                return;
            }
            if (employeeDAO.delete(id)) {
                auditDAO.log("employee", id, "DELETE", sessionId(req), gson.toJson(target), null);
                Json.message(resp, 200, "Employee deleted.");
            } else Json.error(resp, 404, "Employee not found");
        } catch (SQLException e) {
            if (e.getErrorCode() == 1451) Json.error(resp, 409, "Cannot delete: employee is referenced by other records. Deactivate instead.");
            else { e.printStackTrace(); Json.error(resp, 500, "Delete failed."); }
        }
    }

    // ------------------------------------------------------------------ helpers

    private void resetPassword(HttpServletRequest req, HttpServletResponse resp, Long id) throws IOException {
        if (id < 0 || employeeDAO.findById(id) == null) { Json.error(resp, 404, "Employee not found"); return; }
        String pw = Json.raw(Json.readBody(req), "newPassword");
        String err = PasswordPolicy.validate(pw);
        if (err != null) { Json.error(resp, 400, err); return; }
        if (employeeDAO.updatePasswordHash(id, authService.hashPassword(pw))) {
            Json.message(resp, 200, "Password reset. The user's existing sessions are now invalid.");
        } else {
            Json.error(resp, 500, "Update failed.");
        }
    }

    private static String firstError(String... errs) {
        for (String e : errs) if (e != null) return e;
        return null;
    }

    private void createEmployee(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject j = Json.readBody(req);
        if (j == null) { Json.error(resp, 400, "Invalid JSON body"); return; }

        String code = Json.str(j, "employeeCode"), first = Json.str(j, "firstName"), last = Json.str(j, "lastName");
        String gender = Json.str(j, "gender"), email = Json.str(j, "email"), type = Json.str(j, "employmentType");
        String username = Json.str(j, "username"), password = Json.raw(j, "password"), roleStr = Json.str(j, "role");
        String phone = Json.str(j, "phone");
        String status = Json.str(j, "employmentStatus") == null ? "ACTIVE" : Json.str(j, "employmentStatus");
        if (code == null || first == null || last == null || gender == null || email == null || type == null
                || username == null || password == null || roleStr == null || Json.str(j, "dateOfJoining") == null
                || Json.str(j, "departmentId") == null || Json.str(j, "designationId") == null) {
            Json.error(resp, 400, "Invalid data format or missing fields"); return;
        }
        String err = firstError(EmployeeValidator.code(code), EmployeeValidator.name(first, "First name"),
                EmployeeValidator.name(last, "Last name"), EmployeeValidator.gender(gender), EmployeeValidator.email(email),
                EmployeeValidator.phone(phone), EmployeeValidator.type(type), EmployeeValidator.status(status),
                EmployeeValidator.username(username), PasswordPolicy.validate(password));
        if (err != null) { Json.error(resp, 400, err); return; }
        if (EmployeeValidator.EXITED.contains(status)) { Json.error(resp, 400, "A new employee must be ACTIVE or ON_LEAVE."); return; }

        LocalDate joining = EmployeeValidator.parseDate(Json.str(j, "dateOfJoining"));
        LocalDate dob = Json.str(j, "dateOfBirth") == null ? null : EmployeeValidator.parseDate(Json.str(j, "dateOfBirth"));
        if (joining == null || (Json.str(j, "dateOfBirth") != null && dob == null)) {
            Json.error(resp, 400, "Dates must be valid and formatted yyyy-MM-dd."); return;
        }
        err = EmployeeValidator.dates(dob, joining, null, LocalDate.now());
        if (err != null) { Json.error(resp, 400, err); return; }

        try {
            Employee emp = new Employee();
            emp.setEmployeeCode(code);
            emp.setFirstName(first);
            emp.setLastName(last);
            emp.setGender(gender);
            emp.setDateOfBirth(toDate(dob));
            emp.setEmail(email);
            emp.setPhone(phone);
            emp.setDateOfJoining(toDate(joining));
            emp.setEmploymentType(type);
            emp.setEmploymentStatus(status);
            emp.setDepartmentId(j.get("departmentId").getAsLong());
            emp.setDesignationId(j.get("designationId").getAsLong());
            emp.setUsername(username);
            emp.setRole(Role.valueOf(roleStr));
            if (Json.str(j, "reportingManagerId") != null) {
                Long mgrId = j.get("reportingManagerId").getAsLong();
                Employee mgr = employeeDAO.findById(mgrId);
                if (mgr == null || !mgr.isActive() || mgr.getRole() == Role.EMPLOYEE) {
                    Json.error(resp, 400, "Reporting manager must be an active MANAGER or HR_ADMIN."); return;
                }
                emp.setReportingManagerId(mgrId);
            }
            emp.setPasswordHash(authService.hashPassword(password));

            if (employeeDAO.create(emp)) {
                Employee saved = employeeDAO.findByUsername(username);
                if (saved != null) auditDAO.log("employee", saved.getEmployeeId(), "INSERT", sessionId(req), null, gson.toJson(saved));
                Json.message(resp, 201, "Employee created successfully");
            } else Json.error(resp, 500, "Failed to create employee");
        } catch (UnsupportedOperationException | IllegalArgumentException | IllegalStateException e) {
            Json.error(resp, 400, "Invalid data format or missing fields");
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) Json.error(resp, 409, "Employee code, email or username already exists.");
            else if (e.getErrorCode() == 1452) Json.error(resp, 400, "Unknown department, designation or manager.");
            else { e.printStackTrace(); Json.error(resp, 500, "Failed to create employee"); }
        }
    }
}
