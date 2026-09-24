package com.lpms.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.lpms.dao.LeaveApplicationDAO;
import com.lpms.model.LeaveApplication;
import com.lpms.service.LeaveService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

@WebServlet("/api/leaves/*")
public class LeaveServlet extends HttpServlet {

    private LeaveApplicationDAO leaveApplicationDAO;
    private LeaveService leaveService;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        this.leaveApplicationDAO = new LeaveApplicationDAO();
        this.leaveService = new LeaveService();
        this.gson = new GsonBuilder().setDateFormat("yyyy-MM-dd").create();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        Long employeeId = (Long) req.getSession().getAttribute("employeeId");
        
        String pathInfo = req.getPathInfo();
        if (pathInfo != null && pathInfo.equals("/pending")) {
            // Manager viewing pending leaves
            String role = (String) req.getSession().getAttribute("role");
            if ("MANAGER".equals(role) || "HR_ADMIN".equals(role)) {
                List<LeaveApplication> pendingLeaves = leaveApplicationDAO.findPendingForManager(employeeId);
                resp.getWriter().write(gson.toJson(pendingLeaves));
            } else {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.getWriter().write("{\"error\": \"Only managers can view pending leaves.\"}");
            }
        } else {
            // Employee viewing their own history
            List<LeaveApplication> myLeaves = leaveApplicationDAO.findByEmployeeId(employeeId);
            resp.getWriter().write(gson.toJson(myLeaves));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        Long employeeId = (Long) req.getSession().getAttribute("employeeId");

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        JsonObject json = gson.fromJson(sb.toString(), JsonObject.class);
        
        String action = json.has("action") ? json.get("action").getAsString() : "APPLY";
        
        if ("APPROVE".equals(action)) {
            // Manager Approval Flow
            Long leaveApplicationId = json.get("leaveApplicationId").getAsLong();
            String comments = json.has("comments") ? json.get("comments").getAsString() : "";
            
            LeaveApplication app = new LeaveApplication();
            app.setLeaveApplicationId(leaveApplicationId);
            app.setEmployeeId(json.get("applicantEmployeeId").getAsLong());
            app.setLeaveTypeId(json.get("leaveTypeId").getAsLong());
            app.setTotalDays(json.get("totalDays").getAsBigDecimal());
            
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                app.setStartDate(sdf.parse(json.get("startDate").getAsString()));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            if (leaveService.approveLeave(app, employeeId, comments)) {
                resp.getWriter().write("{\"message\": \"Leave approved successfully.\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"error\": \"Failed to approve leave.\"}");
            }
            
        } else if ("REJECT".equals(action)) {
            // Manager Rejection Flow
            Long leaveApplicationId = json.get("leaveApplicationId").getAsLong();
            String comments = json.has("comments") ? json.get("comments").getAsString() : "";
            
            if (leaveApplicationDAO.updateStatus(leaveApplicationId, "REJECTED", employeeId, comments)) {
                resp.getWriter().write("{\"message\": \"Leave rejected successfully.\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"error\": \"Failed to reject leave.\"}");
            }
            
        } else {
            // Apply for Leave Flow
            try {
                LeaveApplication app = new LeaveApplication();
                app.setEmployeeId(employeeId);
                app.setLeaveTypeId(json.get("leaveTypeId").getAsLong());
                
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                app.setStartDate(sdf.parse(json.get("startDate").getAsString()));
                app.setEndDate(sdf.parse(json.get("endDate").getAsString()));
                
                app.setTotalDays(new BigDecimal(json.get("totalDays").getAsString()));
                app.setReason(json.get("reason").getAsString());

                if (leaveApplicationDAO.create(app)) {
                    resp.setStatus(HttpServletResponse.SC_CREATED);
                    resp.getWriter().write("{\"message\": \"Leave application submitted.\"}");
                } else {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    resp.getWriter().write("{\"error\": \"Failed to submit leave application.\"}");
                }
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\": \"Invalid data format\"}");
            }
        }
    }
}
