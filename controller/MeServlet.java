package com.lpms.controller;

import com.google.gson.JsonObject;
import com.lpms.dao.EmployeeDAO;
import com.lpms.model.Employee;
import com.lpms.util.Json;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** GET /api/auth/me - current user (also used by the UI to restore a session after reload). */
@WebServlet("/api/auth/me")
public class MeServlet extends HttpServlet {
    private final EmployeeDAO employeeDAO = new EmployeeDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Employee e = employeeDAO.findById((Long) req.getSession().getAttribute("employeeId"));
        if (e == null) {
            Json.error(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized access. Please log in.");
            return;
        }
        JsonObject out = new JsonObject();
        out.addProperty("employeeId", e.getEmployeeId());
        out.addProperty("employeeCode", e.getEmployeeCode());
        out.addProperty("username", e.getUsername());
        out.addProperty("firstName", e.getFirstName());
        out.addProperty("lastName", e.getLastName());
        out.addProperty("email", e.getEmail());
        out.addProperty("role", e.getRole().name());
        Json.write(resp, HttpServletResponse.SC_OK, out);
    }
}
