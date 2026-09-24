package com.lpms.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.lpms.dao.AttendanceDAO;
import com.lpms.model.Attendance;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@WebServlet("/api/attendance/*")
public class AttendanceServlet extends HttpServlet {

    private AttendanceDAO attendanceDAO;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        this.attendanceDAO = new AttendanceDAO();
        this.gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        Long employeeId = (Long) req.getSession().getAttribute("employeeId");
        
        // Expected format: ?month=2026-09
        String month = req.getParameter("month");
        if (month == null || !month.matches("\\d{4}-\\d{2}")) {
            // Default to current year-month
            month = new SimpleDateFormat("yyyy-MM").format(new Date());
        }

        List<Attendance> attendances = attendanceDAO.findByEmployeeId(employeeId, month);
        resp.getWriter().write(gson.toJson(attendances));
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
        
        try {
            Attendance attendance = new Attendance();
            attendance.setEmployeeId(employeeId);
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            attendance.setAttendanceDate(sdf.parse(json.get("date").getAsString()));
            
            attendance.setCheckIn(new Timestamp(System.currentTimeMillis()));
            attendance.setStatus(json.has("status") ? json.get("status").getAsString() : "PRESENT");
            attendance.setRemarks(json.has("remarks") ? json.get("remarks").getAsString() : "");

            if (attendanceDAO.create(attendance)) {
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().write("{\"message\": \"Attendance marked successfully.\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"error\": \"Failed to mark attendance. You may have already marked attendance for this date.\"}");
            }
        } catch (ParseException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Invalid data format\"}");
        }
    }
}
