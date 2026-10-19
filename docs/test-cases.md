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
| TC-EMP-002   | FR-EMP-001     | Employee     | Add employee with invalid email/phone/DOB       | 400 with a specific validation message, nothing saved.    | Pending      |
| TC-EMP-003   | FR-EMP-002     | Employee     | HR edits department, manager, phone             | Row updated, audit_log UPDATE entry written.              | Pending      |
| TC-EMP-004   | FR-EMP-002     | Employee     | Set reporting manager that creates a loop       | 400 "reporting loop"; no change.                          | Pending      |
| TC-EMP-005   | FR-EMP-003     | Employee     | Search by name/ID/email, filter by dept/status  | Only matching rows returned; Manager sees only reports.   | Pending      |
| TC-EMP-006   | FR-EMP-004     | Employee     | Deactivate employee                             | is_active false, status RESIGNED/TERMINATED, exit date.   | Pending      |
| TC-EMP-007   | FR-EMP-004     | Employee     | Delete employee with attendance/leave history   | 409, record kept; delete allowed only for no-history.     | Pending      |
| TC-EMP-008   | NFR-SEC-002    | Employee     | MANAGER/EMPLOYEE call POST/PUT/DELETE employees | 403 Forbidden.                                            | Pending      |
| TC-EMP-009   | FR-EMP-001     | Employee     | EmployeeValidatorTest (unit)                    | Validation rules pass.                                    | Passed (stand-in runner) |

## Stage 4 - Leave workflow (manual)
- Employee sees per-type entitlement, used, remaining, pending, available.
- Apply: end<start rejected; weekend-only range rejected; overlap with PENDING/APPROVED rejected; over-balance rejected; half-day only single date and only where allowed.
- Manager sees only direct reports' pending requests (HR sees all); cannot act on own request or non-reports (403).
- Approve deducts `used` in the same transaction; reject requires a comment; cancel pending/future-approved restores balance.
- History modal shows SUBMITTED/APPROVED/REJECTED/CANCELLED trail from `leave_history`.
- Re-run `database/schema.sql` to add the `leave_history` table (idempotent).

## Stage 5 - Attendance
| ID | Case | Expected |
|---|---|---|
| ATT-01 | Employee checks in twice on the same day | 2nd rejected (409) |
| ATT-02 | Manager records attendance for non-report / HR record for anyone | 403 / allowed |
| ATT-03 | Record on weekend, holiday, future date, or full-day approved leave | Rejected (400/409) |
| ATT-04 | CSV import with a duplicate and an unknown employee | Valid rows imported; failures listed per row |
| ATT-05 | Month summary with absent, paid leave, unpaid leave, half day | Matches table in docs/attendance-rules.md (unit tests AttendanceRulesTest) |
| ATT-06 | Lock month, then edit/record in it | Rejected (409); unlock restores |
| ATT-07 | Change policy (e.g. MISSING_DAY_TREATMENT=IGNORE) | Summary LOP updates |
