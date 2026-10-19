package com.lpms.controller;

import com.google.gson.JsonObject;
import com.lpms.dao.EmployeeDAO;
import com.lpms.model.Employee;
import com.lpms.service.AuthService;
import com.lpms.util.Json;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/** POST /api/auth/change-password {currentPassword, newPassword} - self-service. */
@WebServlet("/api/auth/change-password")
public class ChangePasswordServlet extends HttpServlet {
    private final AuthService authService = new AuthService();
    private final EmployeeDAO employeeDAO = new EmployeeDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body = Json.readBody(req);
        String current = Json.raw(body, "currentPassword");
        String next = Json.raw(body, "newPassword");
        if (current == null || next == null) {
            Json.error(resp, HttpServletResponse.SC_BAD_REQUEST, "Current and new password are required.");
            return;
        }
        HttpSession session = req.getSession();
        Long id = (Long) session.getAttribute("employeeId");
        String err = authService.changePassword(id, current, next);
        if (err != null) {
            Json.error(resp, HttpServletResponse.SC_BAD_REQUEST, err);
            return;
        }
        // Keep this session alive (new stamp, new session id); other sessions of this user become invalid.
        Employee e = employeeDAO.findById(id);
        session.setAttribute("pwdStamp", AuthService.stamp(e.getPasswordHash()));
        req.changeSessionId();
        Json.message(resp, HttpServletResponse.SC_OK, "Password changed successfully.");
    }
}
