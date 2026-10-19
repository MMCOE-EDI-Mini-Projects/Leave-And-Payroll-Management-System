package com.lpms.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/** Protects the application shell: unauthenticated users are redirected to login.html. */
@WebFilter({"/index.html", "/js/app.js"})
public class PageAccessFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);
        if (session == null || !(session.getAttribute("employeeId") instanceof Long)) {
            res.sendRedirect(req.getContextPath() + "/login.html");
            return;
        }
        res.setHeader("Cache-Control", "no-store"); // back button after logout must not show cached pages
        res.setHeader("X-Frame-Options", "DENY");
        chain.doFilter(request, response);
    }
}
