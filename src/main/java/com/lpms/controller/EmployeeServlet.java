package com.lpms.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.lpms.dao.EmployeeDAO;
import com.lpms.model.Employee;
import com.lpms.model.Role;
import com.lpms.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

@WebServlet("/api/employees/*")
public class EmployeeServlet extends HttpServlet {

    private EmployeeDAO employeeDAO;
    private AuthService authService;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        this.employeeDAO = new EmployeeDAO();
        this.authService = new AuthService();
        this.gson = new GsonBuilder().setDateFormat("yyyy-MM-dd").create();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            // Get all employees
            List<Employee> employees = employeeDAO.findAll();
            resp.getWriter().write(gson.toJson(employees));
        } else {
            // Get employee by ID
            try {
                Long id = Long.parseLong(pathInfo.substring(1));
                Employee employee = employeeDAO.findById(id);
                if (employee != null) {
                    resp.getWriter().write(gson.toJson(employee));
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("{\"error\": \"Employee not found\"}");
                }
            } catch (NumberFormatException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\": \"Invalid employee ID\"}");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        // Role check (Only HR_ADMIN can add employees)
        String sessionRole = (String) req.getSession().getAttribute("role");
        if (!"HR_ADMIN".equals(sessionRole)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.getWriter().write("{\"error\": \"Only HR_ADMIN can add employees.\"}");
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
        
        try {
            Employee emp = new Employee();
            emp.setEmployeeCode(json.get("employeeCode").getAsString());
            emp.setFirstName(json.get("firstName").getAsString());
            emp.setLastName(json.get("lastName").getAsString());
            emp.setGender(json.get("gender").getAsString());
            emp.setEmail(json.get("email").getAsString());
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            emp.setDateOfJoining(sdf.parse(json.get("dateOfJoining").getAsString()));
            
            emp.setEmploymentType(json.get("employmentType").getAsString());
            emp.setDepartmentId(json.get("departmentId").getAsLong());
            emp.setDesignationId(json.get("designationId").getAsLong());
            
            emp.setUsername(json.get("username").getAsString());
            // Hash the password before saving
            String plainPassword = json.get("password").getAsString();
            emp.setPasswordHash(authService.hashPassword(plainPassword));
            
            emp.setRole(Role.valueOf(json.get("role").getAsString()));

            if (employeeDAO.create(emp)) {
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().write("{\"message\": \"Employee created successfully\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"error\": \"Failed to create employee\"}");
            }
        } catch (ParseException | NullPointerException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Invalid data format or missing fields\"}");
        }
    }
}
