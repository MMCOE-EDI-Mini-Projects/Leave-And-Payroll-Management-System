package com.lpms.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@WebFilter("/api/*")
public class AuthenticationFilter implements Filter {

    // Paths that do not require authentication
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/api/auth/login",
            "/api/auth/logout"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI().substring(req.getContextPath().length());

        // Allow excluded paths (e.g., login)
        if (EXCLUDED_PATHS.contains(path)) {
            chain.doFilter(request, response);
            return;
        }

        // Check for session
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("employeeId") == null) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\": \"Unauthorized access. Please log in.\"}");
            return;
        }

        // Basic RBAC - Optional to implement specific role checks based on paths here
        String role = (String) session.getAttribute("role");
        if (path.startsWith("/api/admin/") && !"HR_ADMIN".equals(role)) {
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\": \"Forbidden access. Admin role required.\"}");
            return;
        }
        
        if (path.startsWith("/api/manager/") && (!"MANAGER".equals(role) && !"HR_ADMIN".equals(role))) {
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\": \"Forbidden access. Manager role required.\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
