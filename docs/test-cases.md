# LPMS Test Cases

| Test Case ID | Requirement ID | Module       | Test Scenario                                   | Expected Result                                           | Status       |
|--------------|----------------|--------------|-------------------------------------------------|-----------------------------------------------------------|--------------|
| TC-AUTH-001  | FR-AUTH-001    | Auth         | Valid login credentials                         | System grants access, creates session, redirects to DB.   | Pending      |
| TC-AUTH-002  | FR-AUTH-001    | Auth         | Invalid password                                | System denies access, shows "Invalid username/password".  | Pending      |
| TC-AUTH-003  | NFR-SEC-001    | Auth         | Session timeout after 30 mins                   | System invalidates session and forces re-login.           | Pending      |
| TC-EMP-001   | FR-EMP-001     | Employee     | Add new employee (HR_ADMIN only)                | Record created in database successfully.                  | Pending      |
| TC-LEAVE-001 | FR-LEAVE-001   | Leave        | Apply for leave (Start Date < End Date)         | Application submitted, status is PENDING.                 | Pending      |
| TC-LEAVE-002 | FR-LEAVE-001   | Leave        | Apply for leave (End Date < Start Date)         | System rejects application with error message.            | Pending      |
| TC-LEAVE-003 | FR-LEAVE-002   | Leave        | Manager approves pending leave                  | Status changes to APPROVED, leave balance is updated.     | Pending      |
| TC-ATT-001   | FR-ATT-001     | Attendance   | Mark daily attendance                           | Attendance record inserted successfully.                  | Pending      |
| TC-PAY-001   | FR-PAY-001     | Payroll      | Calculate flat salary component                 | Rule engine returns exact flat amount.                    | Passed       |
| TC-PAY-002   | FR-PAY-001     | Payroll      | Calculate percentage salary component           | Rule engine calculates correct percentage of gross.       | Passed       |
| TC-PAY-003   | FR-PAY-001     | Payroll      | Calculate LOP deduction                         | Rule engine deducts correctly based on LOP days.          | Passed       |
| TC-PAY-004   | FR-PAY-002     | Payroll      | Run monthly payroll batch                       | Employee_payroll records generated successfully.          | Pending      |
