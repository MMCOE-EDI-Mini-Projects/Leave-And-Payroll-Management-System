package com.lpms.controller;

import com.google.gson.JsonObject;
import com.lpms.dao.EmployeeDAO;
import com.lpms.model.Employee;
import com.lpms.service.AuthService;
import com.lpms.util.Json;
import com.lpms.util.LoginThrottle;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/api/auth/login")
public class LoginServlet extends HttpServlet {

    private static final LoginThrottle THROTTLE = new LoginThrottle();
    private final AuthService authService = new AuthService();
    private final EmployeeDAO employeeDAO = new EmployeeDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body = Json.readBody(req);
        String username = Json.str(body, "username");
        String password = Json.raw(body, "password");
        if (username == null || password == null || password.isEmpty()) {
            Json.error(resp, HttpServletResponse.SC_BAD_REQUEST, "Username and password are required.");
            return;
        }

        String key = username.toLowerCase() + "|" + req.getRemoteAddr();
        long locked = THROTTLE.secondsLocked(key);
        if (locked > 0) {
            resp.setHeader("Retry-After", String.valueOf(locked));
            Json.error(resp, 429, "Too many failed attempts. Try again in " + ((locked + 59) / 60) + " minute(s).");
            return;
        }

        Employee employee = authService.authenticate(username, password);
        if (employee == null) {
            THROTTLE.recordFailure(key);
            Json.error(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
            return;
        }
        THROTTLE.recordSuccess(key);

        // Session fixation defence: discard any pre-login session and start a fresh one.
        HttpSession old = req.getSession(false);
        if (old != null) old.invalidate();
        HttpSession session = req.getSession(true);
        session.setMaxInactiveInterval(30 * 60);
        session.setAttribute("employeeId", employee.getEmployeeId());
        session.setAttribute("role", employee.getRole().name());
        session.setAttribute("pwdStamp", AuthService.stamp(employee.getPasswordHash()));
        employeeDAO.updateLastLogin(employee.getEmployeeId());

        JsonObject out = new JsonObject();
        out.addProperty("message", "Login successful");
        out.addProperty("employeeId", employee.getEmployeeId());
        out.addProperty("username", employee.getUsername());
        out.addProperty("role", employee.getRole().name());
        out.addProperty("firstName", employee.getFirstName());
        out.addProperty("lastName", employee.getLastName());
        Json.write(resp, HttpServletResponse.SC_OK, out);
    }
}
