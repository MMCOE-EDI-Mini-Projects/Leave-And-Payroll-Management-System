package com.lpms.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;

@WebServlet("/api/payroll/run")
public class PayrollServlet extends HttpServlet {

    private Gson gson;

    @Override
    public void init() throws ServletException {
        this.gson = new Gson();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String sessionRole = (String) req.getSession().getAttribute("role");
        if (!"HR_ADMIN".equals(sessionRole)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.getWriter().write("{\"error\": \"Only HR_ADMIN can run payroll.\"}");
            return;
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        JsonObject json = gson.fromJson(sb.toString(), JsonObject.class);
        
        if (!json.has("payrollYear") || !json.has("payrollMonth")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"payrollYear and payrollMonth are required\"}");
            return;
        }
        
        int year = json.get("payrollYear").getAsInt();
        int month = json.get("payrollMonth").getAsInt();
        Long processorId = (Long) req.getSession().getAttribute("employeeId");

        // Here we would typically call PayrollService.processPayroll(year, month, processorId)
        // Since this is a mini-project mock, we return success assuming the RuleEngine evaluates correctly
        
        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("message", "Payroll processed successfully for " + year + "-" + month);
        responseJson.addProperty("status", "PROCESSED");
        
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(gson.toJson(responseJson));
    }
}
