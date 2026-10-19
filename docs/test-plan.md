# Test Plan: LPMS

## 1. Introduction
This test plan outlines the testing strategy for the Leave and Payroll Management System (LPMS) developed for the EDI Semester V mini-project.

## 2. Test Strategy
Testing will be performed using a combination of manual and automated testing:
- **Unit Testing**: JUnit will be used for core business logic (e.g., `PayrollRuleEngine`).
- **API Testing**: Postman or manual fetch calls via browser console to test Servlet endpoints.
- **UI/Integration Testing**: Manual browser testing using Google Chrome Developer Tools.
- **Load Testing**: Apache JMeter will be used to simulate 50-100 concurrent users performing login, viewing dashboards, and marking attendance.

## 3. Test Scope
- Authentication and Role-Based Access Control.
- Employee Master Data Management.
- Leave Application and Approval workflows.
- Attendance Capture and LOP calculation.
- Payroll Engine formula accuracy.
- Database transaction safety (Rollbacks during concurrent access).

## 4. Test Deliverables
- Test Plan (this document)
- Test Cases (`test-cases.md`)
- Test Execution Results (`test-results.md`)
