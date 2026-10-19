-- LPMS reference/seed data. Safe to re-run (idempotent). Run after schema.sql.
USE lpms;

-- 1. Roles
INSERT IGNORE INTO role (role_code, role_name, description) VALUES
('EMPLOYEE','Employee','Self-service: own leave, attendance, payslips'),
('MANAGER','Manager','Approves leave and views team attendance'),
('HR_ADMIN','HR Administrator','Full access: employees, payroll, configuration');

-- 2. Departments
INSERT IGNORE INTO department (department_code, department_name, description) VALUES
('HR','Human Resources','People operations'),
('ENG','Engineering','Software development'),
('FIN','Finance','Accounts and payroll');

-- 3. Designations
INSERT IGNORE INTO designation (designation_code, designation_name, job_level) VALUES
('HR_MGR','HR Manager','Manager'),
('ENG_MGR','Engineering Manager','Manager'),
('SWE','Software Engineer','Mid'),
('ACC','Accountant','Mid');

-- 4. Leave Types
INSERT IGNORE INTO leave_type (leave_code, leave_name, annual_entitlement, is_paid, allows_half_day, allows_carry_forward, max_carry_forward) VALUES
('CL','Casual Leave',12,TRUE,TRUE,FALSE,0),
('SL','Sick Leave',10,TRUE,TRUE,FALSE,0),
('EL','Earned Leave',15,TRUE,FALSE,TRUE,30),
('LOP','Loss of Pay',0,FALSE,TRUE,FALSE,0);

-- 5. Salary Components
INSERT IGNORE INTO salary_component (component_code, component_name, component_type, calculation_method, is_taxable, is_pf_applicable) VALUES
('BASIC','Basic Salary','EARNING','PERCENTAGE',TRUE,TRUE),
('HRA','House Rent Allowance','EARNING','PERCENTAGE',TRUE,FALSE),
('SPECIAL','Special Allowance','EARNING','FLAT',TRUE,FALSE),
('PF_EMP','Provident Fund (Employee)','DEDUCTION','PERCENTAGE',FALSE,FALSE),
('PROF_TAX','Professional Tax','DEDUCTION','FLAT',FALSE,FALSE),
('TDS','Income Tax (TDS)','DEDUCTION','FORMULA',FALSE,FALSE),
('PF_EMPR','Provident Fund (Employer)','EMPLOYER_CONTRIBUTION','PERCENTAGE',FALSE,FALSE);

-- 6. Employees:
-- Password for all seed users is 'Admin@123' (BCrypt hash: $2a$10$VE5ovp/u6r.SYc.iX5VdhefvTDtjoUxWRV0kGSP8ysluFzM5IjhD.)

-- HR Admin: admin / Admin@123
INSERT INTO employee (employee_code, first_name, last_name, gender, email, date_of_joining,
    department_id, designation_id, username, password_hash, role)
SELECT 'EMP0001','System','Admin','OTHER','admin@lpms.local','2025-01-01',
    (SELECT department_id FROM department WHERE department_code='HR'),
    (SELECT designation_id FROM designation WHERE designation_code='HR_MGR'),
    'admin','$2a$10$VE5ovp/u6r.SYc.iX5VdhefvTDtjoUxWRV0kGSP8ysluFzM5IjhD.','HR_ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM employee WHERE username='admin');

-- Manager: manager / Admin@123
INSERT INTO employee (employee_code, first_name, last_name, gender, email, date_of_joining,
    department_id, designation_id, username, password_hash, role)
SELECT 'EMP0002','Priya','Sharma','FEMALE','manager@lpms.local','2025-01-15',
    (SELECT department_id FROM department WHERE department_code='ENG'),
    (SELECT designation_id FROM designation WHERE designation_code='ENG_MGR'),
    'manager','$2a$10$VE5ovp/u6r.SYc.iX5VdhefvTDtjoUxWRV0kGSP8ysluFzM5IjhD.','MANAGER'
WHERE NOT EXISTS (SELECT 1 FROM employee WHERE username='manager');

-- Employee: employee / Admin@123 (Reports to manager)
INSERT INTO employee (employee_code, first_name, last_name, gender, email, date_of_joining,
    department_id, designation_id, reporting_manager_id, username, password_hash, role)
SELECT 'EMP0003','Rahul','Verma','MALE','employee@lpms.local','2025-02-01',
    (SELECT department_id FROM department WHERE department_code='ENG'),
    (SELECT designation_id FROM designation WHERE designation_code='SWE'),
    (SELECT employee_id FROM employee WHERE username='manager'),
    'employee','$2a$10$VE5ovp/u6r.SYc.iX5VdhefvTDtjoUxWRV0kGSP8ysluFzM5IjhD.','EMPLOYEE'
WHERE NOT EXISTS (SELECT 1 FROM employee WHERE username='employee');

-- 7. Leave Balances for 2026
INSERT IGNORE INTO leave_balance (employee_id, leave_type_id, leave_year, opening_balance, accrued, used)
SELECT e.employee_id, lt.leave_type_id, 2026, lt.annual_entitlement, 0, 0
FROM employee e
CROSS JOIN leave_type lt
WHERE lt.is_paid = TRUE;

-- 8. Tax Slabs (Indian New Tax Regime FY 2025-2026)
INSERT IGNORE INTO tax_slab (tax_regime, financial_year, slab_order, lower_limit, upper_limit, tax_rate, cess_rate) VALUES
('NEW', '2025-2026', 1, 0.00, 300000.00, 0.00, 4.00),
('NEW', '2025-2026', 2, 300000.00, 700000.00, 5.00, 4.00),
('NEW', '2025-2026', 3, 700000.00, 1000000.00, 10.00, 4.00),
('NEW', '2025-2026', 4, 1000000.00, 1200000.00, 15.00, 4.00),
('NEW', '2025-2026', 5, 1200000.00, 1500000.00, 20.00, 4.00),
('NEW', '2025-2026', 6, 1500000.00, NULL, 30.00, 4.00);

-- 9. Attendance / LOP policy defaults
INSERT IGNORE INTO attendance_policy (policy_key, policy_value, description) VALUES
('MISSING_DAY_TREATMENT','ABSENT','Working day with no attendance record and no approved leave: ABSENT (counts as LOP) or IGNORE'),
('LATE_AFTER_TIME','09:30','Check-in after this time (HH:mm) is a late mark'),
('LATES_PER_HALF_DAY_LOP','0','Every N late marks in a month = 0.5 day LOP (0 disables)'),
('STANDARD_WORK_MINUTES','480','Minutes in a standard day; worked time beyond it is overtime'),
('BACKDATE_LIMIT_DAYS','31','How many days back a manager may record/correct attendance (HR is limited only by month locks)');

-- 10. Sample Salary Structure
-- Admin: CTC 12,00,000, Gross 1,00,000/mo
INSERT INTO employee_salary (employee_id, annual_ctc, monthly_gross, effective_from, is_active)
SELECT employee_id, 1200000.00, 100000.00, '2025-01-01', TRUE FROM employee WHERE username='admin'
AND NOT EXISTS (SELECT 1 FROM employee_salary WHERE employee_id = (SELECT employee_id FROM employee WHERE username='admin'));

-- Manager: CTC 9,60,000, Gross 80,000/mo
INSERT INTO employee_salary (employee_id, annual_ctc, monthly_gross, effective_from, is_active)
SELECT employee_id, 960000.00, 80000.00, '2025-01-15', TRUE FROM employee WHERE username='manager'
AND NOT EXISTS (SELECT 1 FROM employee_salary WHERE employee_id = (SELECT employee_id FROM employee WHERE username='manager'));

-- Employee: CTC 6,00,000, Gross 50,000/mo
INSERT INTO employee_salary (employee_id, annual_ctc, monthly_gross, effective_from, is_active)
SELECT employee_id, 600000.00, 50000.00, '2025-02-01', TRUE FROM employee WHERE username='employee'
AND NOT EXISTS (SELECT 1 FROM employee_salary WHERE employee_id = (SELECT employee_id FROM employee WHERE username='employee'));

-- Employee Salary Details (Basic 50%, HRA 40% of basic, Special 5000, PF 12%, PT 200)
INSERT IGNORE INTO employee_salary_detail (employee_salary_id, component_id, component_value)
SELECT es.employee_salary_id, sc.component_id,
    CASE 
        WHEN sc.component_code = 'BASIC' THEN 50.00
        WHEN sc.component_code = 'HRA' THEN 40.00
        WHEN sc.component_code = 'SPECIAL' THEN 5000.00
        WHEN sc.component_code = 'PF_EMP' THEN 12.00
        WHEN sc.component_code = 'PROF_TAX' THEN 200.00
        ELSE 0.00
    END
FROM employee_salary es
CROSS JOIN salary_component sc
WHERE sc.component_code IN ('BASIC', 'HRA', 'SPECIAL', 'PF_EMP', 'PROF_TAX');
