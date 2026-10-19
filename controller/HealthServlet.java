package com.lpms.controller;

import com.lpms.util.DBUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Public health check: GET /health -> reports app and database status. */
@WebServlet("/health")
public class HealthServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        boolean db = DBUtil.isDatabaseAvailable();
        resp.setContentType("application/json");
        resp.setStatus(db ? HttpServletResponse.SC_OK : HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        resp.getWriter().write("{\"app\":\"UP\",\"database\":\"" + (db ? "UP" : "DOWN") + "\"}");
    }
}
