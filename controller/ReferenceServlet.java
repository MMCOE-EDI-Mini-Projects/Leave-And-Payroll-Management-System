package com.lpms.controller;

import com.lpms.dao.ReferenceDAO;
import com.lpms.util.Json;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** GET /api/reference - departments, designations, managers (HR_ADMIN only; enforced in AuthenticationFilter). */
@WebServlet("/api/reference")
public class ReferenceServlet extends HttpServlet {
    private final ReferenceDAO dao = new ReferenceDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Json.write(resp, 200, dao.all());
        } catch (SQLException e) {
            e.printStackTrace();
            Json.error(resp, 500, "Failed to load reference data");
        }
    }
}
