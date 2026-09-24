package com.lpms.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/api/auth/logout")
public class LogoutServlet extends HttpServlet {

    private Gson gson;

    @Override
    public void init() throws ServletException {
        this.gson = new Gson();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate(); // Invalidate the session on logout
        }

        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("message", "Logout successful");

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(gson.toJson(responseJson));
    }
}
