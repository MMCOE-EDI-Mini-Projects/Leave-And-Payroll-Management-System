package com.lpms.controller;

import com.lpms.util.Json;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/api/auth/logout")
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();

        Cookie cookie = new Cookie("JSESSIONID", "");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        String ctx = req.getContextPath();
        cookie.setPath(ctx.isEmpty() ? "/" : ctx);
        resp.addCookie(cookie);

        Json.message(resp, HttpServletResponse.SC_OK, "Logout successful");
    }
}
