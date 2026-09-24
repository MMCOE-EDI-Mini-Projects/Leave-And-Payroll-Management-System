package com.lpms.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lpms.model.Employee;
import com.lpms.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;

@WebServlet("/api/auth/login")
public class LoginServlet extends HttpServlet {

    private AuthService authService;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        this.authService = new AuthService();
        this.gson = new Gson();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        JsonObject jsonObject = gson.fromJson(sb.toString(), JsonObject.class);
        if (jsonObject == null || !jsonObject.has("username") || !jsonObject.has("password")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Username and password are required.\"}");
            return;
        }

        String username = jsonObject.get("username").getAsString();
        String password = jsonObject.get("password").getAsString();

        Employee employee = authService.authenticate(username, password);

        if (employee != null) {
            // Create session
            HttpSession session = req.getSession(true);
            session.setAttribute("employeeId", employee.getEmployeeId());
            session.setAttribute("role", employee.getRole().name());

            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("message", "Login successful");
            responseJson.addProperty("employeeId", employee.getEmployeeId());
            responseJson.addProperty("role", employee.getRole().name());
            responseJson.addProperty("firstName", employee.getFirstName());
            responseJson.addProperty("lastName", employee.getLastName());

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(gson.toJson(responseJson));
        } else {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"error\": \"Invalid username or password.\"}");
        }
    }
}
