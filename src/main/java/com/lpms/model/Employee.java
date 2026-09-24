package com.lpms.model;

import java.util.Date;

public class Employee {
    private Long employeeId;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String gender;
    private Date dateOfBirth;
    private String email;
    private String phone;
    private Date dateOfJoining;
    private Date dateOfExit;
    private String employmentType;
    private String employmentStatus;
    
    private Long departmentId;
    private Long designationId;
    private Long reportingManagerId;
    
    private String username;
    private String passwordHash;
    private Role role;
    private boolean isActive;
    private Date lastLoginAt;

    // Default constructor
    public Employee() {}

    // Getters and Setters
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Date getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(Date dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Date getDateOfJoining() { return dateOfJoining; }
    public void setDateOfJoining(Date dateOfJoining) { this.dateOfJoining = dateOfJoining; }

    public Date getDateOfExit() { return dateOfExit; }
    public void setDateOfExit(Date dateOfExit) { this.dateOfExit = dateOfExit; }

    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }

    public String getEmploymentStatus() { return employmentStatus; }
    public void setEmploymentStatus(String employmentStatus) { this.employmentStatus = employmentStatus; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public Long getDesignationId() { return designationId; }
    public void setDesignationId(Long designationId) { this.designationId = designationId; }

    public Long getReportingManagerId() { return reportingManagerId; }
    public void setReportingManagerId(Long reportingManagerId) { this.reportingManagerId = reportingManagerId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Date getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Date lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
