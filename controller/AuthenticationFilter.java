package com.lpms.controller;

import com.lpms.dao.EmployeeDAO;
import com.lpms.model.Employee;
import com.lpms.service.AuthService;
import com.lpms.util.Json;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Guards every /api/* request: CSRF header, valid session, account still active,
 * password unchanged since login, and coarse role-based path rules.
 * The role is always re-read from the database, so role changes/deactivation apply immediately.
 */
@WebFilter("/api/*")
public class AuthenticationFilter implements Filter {

    public static final String CSRF_HEADER = "X-Requested-With";
    public static final String CSRF_VALUE = "LPMS";

    private static final List<String> PUBLIC_PATHS = Arrays.asList("/api/auth/login", "/api/auth/logout");
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final EmployeeDAO employeeDAO = new EmployeeDAO();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        res.setHeader("Cache-Control", "no-store");
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader("X-Frame-Options", "DENY");

        String path = req.getRequestURI().substring(req.getContextPath().length());

        // CSRF defence: state-changing requests must carry a custom header (cannot be set cross-site without CORS).
        if (!SAFE_METHODS.contains(req.getMethod()) && !CSRF_VALUE.equals(req.getHeader(CSRF_HEADER))) {
            Json.error(res, HttpServletResponse.SC_FORBIDDEN, "Missing or invalid request header.");
            return;
        }

        if (PUBLIC_PATHS.contains(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        Object idAttr = (session == null) ? null : session.getAttribute("employeeId");
        if (!(idAttr instanceof Long)) {
            Json.error(res, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized access. Please log in.");
            return;
        }

        Employee current;
        try {
            current = employeeDAO.findActiveForSession((Long) idAttr);
        } catch (SQLException e) {
            e.printStackTrace();
            Json.error(res, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Service temporarily unavailable.");
            return;
        }
        if (current == null || !AuthService.stamp(current.getPasswordHash()).equals(session.getAttribute("pwdStamp"))) {
            session.invalidate();
            Json.error(res, HttpServletResponse.SC_UNAUTHORIZED, "Session expired. Please log in again.");
            return;
        }

        String role = current.getRole().name();
        session.setAttribute("role", role); // keep in sync with DB

        if (!allowed(path, role)) {
            Json.error(res, HttpServletResponse.SC_FORBIDDEN, "Forbidden: insufficient role.");
            return;
        }
        chain.doFilter(request, response);
    }

    /** Coarse path rules; servlets add fine-grained (ownership/team) checks. */
    static boolean allowed(String path, String role) {
        boolean admin = "HR_ADMIN".equals(role);
        boolean manager = "MANAGER".equals(role);
        if (path.startsWith("/api/admin/") || path.equals("/api/reference")) {
            return admin;
        }
        if (path.startsWith("/api/payroll/")) {
            // Employee self-service payslip views are permitted for all authenticated users
            if (path.equals("/api/payroll/my-payslips") || path.equals("/api/payroll/payslip")) {
                return true;
            }
            return admin;
        }
        if (path.startsWith("/api/reports/")) {
            if (path.contains("/my") || path.contains("/payslip")) {
                return true;
            }
            return admin || manager;
        }
        if (path.startsWith("/api/manager/")) return admin || manager;
        return true;
    }
}
