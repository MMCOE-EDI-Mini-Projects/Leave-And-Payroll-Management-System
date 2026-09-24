# Software Requirements Specification (SRS)
**Project Title:** Leave and Payroll Management System (LPMS)

## 1. Introduction
### 1.1 Purpose
This document specifies the software requirements for the Leave and Payroll Management System (LPMS). It serves as a guide for development and academic evaluation.

### 1.2 Scope
LPMS is an integrated 3-tier web application designed to automate HR processes for a small-to-medium organization. Key features include employee management, leave tracking (application and manager approval), attendance capture, and a configurable rule-based payroll processing engine.

## 2. Overall System Description
### 2.1 Product Perspective
LPMS replaces manual spreadsheets. It consists of a vanilla HTML/JS client, Java Servlets backend, and MySQL database, strictly following the MVC design pattern.

### 2.2 User Classes
1. **Employee**: Applies for leave, views attendance and payslips.
2. **Manager**: Approves/rejects leaves for their direct reports.
3. **HR/Admin**: Manages master data, runs payroll, and generates organization-wide reports.

## 3. Functional Requirements

| Req ID        | Description                                                                 | Priority |
|---------------|-----------------------------------------------------------------------------|----------|
| FR-AUTH-001   | The system must support role-based secure login using BCrypt passwords.     | High     |
| FR-EMP-001    | HR Admin must be able to add, update, and manage employee master records.   | High     |
| FR-LEAVE-001  | Employees must be able to apply for leave specifying dates and reason.      | High     |
| FR-LEAVE-002  | Managers must be able to approve or reject pending leaves.                  | High     |
| FR-ATT-001    | Employees can mark daily attendance. System calculates LOP days.            | High     |
| FR-PAY-001    | System must calculate salary components dynamically based on rules.         | High     |
| FR-PAY-002    | System must generate monthly batch payrolls preventing duplicate runs.      | High     |

## 4. Non-Functional Requirements
- **NFR-SEC-001**: Session timeout must occur after 30 minutes of inactivity.
- **NFR-PERF-001**: Page load times and API responses should ideally be under 2 seconds.
- **NFR-ARCH-001**: The system must adhere to a strict 3-tier architecture.
