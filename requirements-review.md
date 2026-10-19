# Requirements and Workspace Review Report
**Project Theme:** Leave and Payroll Management System (LPMS)

## 1. Workspace Inspection
**What is already available:**
- `EDI Project Guidelines for Students.pptx (1).pdf`: Contains academic project timeline, assessment guidelines, and rubrics.
- `Leave_Payroll_Management_System.docx.pdf`: Contains the problem statement, scope, module breakdown, and required technology stack.
- `new schema lpms.pdf`: Contains a normalized 3NF database design with 15 SQL `CREATE TABLE` definitions.

**What is missing:**
- No Java/Servlet backend code or Maven `pom.xml` configuration exists.
- No HTML/CSS/JavaScript frontend files exist.
- No `docs/` directory or SRS documentation exists yet.
- No SQL setup scripts (only PDF documentation of the schema).

## 2. Database Schema Review & Corrections
The proposed schema in `new schema lpms.pdf` contains a well-normalized 3NF structure. However, there are a few issues and discrepancies that need correction:

1. **Table Count Mismatch:** The document claims 13 new tables but actually contains 15 CREATE TABLE statements (including `payroll_item` and `audit_log`).
2. **`employee_salary_detail` (Component Value):** The field `amount DECIMAL(15,2)` implies a fixed monetary value. However, `salary_component` supports `calculation_method ENUM('FLAT', 'PERCENTAGE', 'FORMULA')`. 
   * **Correction:** Rename `amount` to `component_value` in `employee_salary_detail` to clarify that this could store a percentage (e.g., `12.00` for 12% PF) or a flat amount depending on the component configuration.
3. **`attendance` (Locking Past Records):** The current table allows updates indefinitely. Once payroll is processed, modifying past attendance would break audit trails and cause discrepancies with generated payslips.
   * **Correction:** Add a boolean flag `is_locked BOOLEAN DEFAULT FALSE` to the `attendance` table to freeze records once the payroll cycle for that month is finalized.
4. **`leave_application` (Manager Feedback):** The schema uses `manager_comments VARCHAR(500)`. It's well-designed, but it's important to enforce that `approver_id` and `actioned_at` are populated when the status moves away from `PENDING`. We will handle this constraint at the application level.
5. **`payroll_run` (Scope):** `uq_prun_period UNIQUE (payroll_year, payroll_month)` restricts the system to one payroll run per month globally. This is appropriate for a mini-project scope.

## 3. Potential Technical Risks
- **Concurrency in Leave & Payroll:** Two users applying for leave simultaneously, or HR processing payroll while attendance is being imported, could cause race conditions. We need strict database transactions (JDBC `Connection.setAutoCommit(false)`).
- **Session Management:** Using plain Java `HttpSession` requires careful tracking of idle timeouts and avoiding storing large objects in the session to meet the 50-user concurrency target.
- **Vanilla JS & UI Scaling:** Managing DOM manipulation and API calls without a framework requires strict adherence to modular JavaScript to avoid spaghetti code.
- **Date/Time Handling:** Accurately calculating `lop_days` and overlapping leave dates across different months can introduce off-by-one errors.

## 4. Recommended Implementation Order
1. **Phase 1: Planning & Setup** (Current)
   - Finalize SRS, review schema, and initialize the project repository.
2. **Phase 2: Database & Backend Foundation**
   - Create the MySQL schema using the corrected design.
   - Set up the Maven webapp project, Apache Tomcat, and JDBC utilities.
3. **Phase 3: Core Modules (Auth & Employee)**
   - Implement login, session management, and role-based access.
   - Implement Employee CRUD operations.
4. **Phase 4: Leave & Attendance Management**
   - Implement leave application, workflow, and balance tracking.
   - Implement attendance recording and summary generation.
5. **Phase 5: Payroll Engine**
   - Implement salary configuration, payroll calculation rules, and payslip generation.
6. **Phase 6: Frontend Integration**
   - Build HTML/CSS UI and connect vanilla JavaScript fetch calls to Servlet APIs.
7. **Phase 7: Testing & Documentation**
   - Execute test cases, perform load testing with JMeter, and finalize academic documentation (DFDs, User Manual).
