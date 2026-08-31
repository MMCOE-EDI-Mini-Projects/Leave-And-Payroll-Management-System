-- =========================================================
-- LEAVE AND PAYROLL MANAGEMENT SYSTEM (LPMS)
-- FULL DATABASE SCHEMA — ALL 30 TABLES
-- =========================================================
-- Module 1 : Employee Management
-- Module 2 : Authentication / Authorization
-- Module 3 : Leave
-- Module 4 : Attendance
-- Module 5 : Payroll
-- Module 6 : Audit / System
-- Module 7 : Admin / Configuration
-- =========================================================

CREATE DATABASE IF NOT EXISTS lpms;

USE lpms;

-- =========================================================
-- MODULE 1 : EMPLOYEE MANAGEMENT
-- =========================================================
-- department, designation, employee, employment_assignment,
-- employment_status_history

-- 1. DEPARTMENT

CREATE TABLE department (
    department_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    department_code VARCHAR(30) NOT NULL UNIQUE,
    department_name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    parent_department_id BIGINT UNSIGNED,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_department_parent
        FOREIGN KEY (parent_department_id)
        REFERENCES department(department_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    INDEX idx_department_parent (parent_department_id),
    INDEX idx_department_active (is_active)
);

-- 2. DESIGNATION

CREATE TABLE designation (
    designation_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    designation_code VARCHAR(30) NOT NULL UNIQUE,
    designation_name VARCHAR(100) NOT NULL UNIQUE,
    job_level VARCHAR(30),
    description VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_designation_active (is_active)
);

-- 3. EMPLOYEE

CREATE TABLE employee (
    employee_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    employee_code VARCHAR(30) NOT NULL UNIQUE,

    first_name VARCHAR(60) NOT NULL,
    middle_name VARCHAR(60),
    last_name VARCHAR(60) NOT NULL,

    date_of_birth DATE,
    gender VARCHAR(20),

    email VARCHAR(255) UNIQUE,
    phone VARCHAR(20),

    date_of_joining DATE NOT NULL,
    date_of_exit DATE,

    employment_status VARCHAR(30) NOT NULL,

    version BIGINT UNSIGNED NOT NULL DEFAULT 1,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_employee_dates
        CHECK (
            date_of_exit IS NULL
            OR date_of_exit >= date_of_joining
        ),

    CONSTRAINT chk_employee_version
        CHECK (version >= 1),

    INDEX idx_employee_status (employment_status),
    INDEX idx_employee_joining (date_of_joining)
);

-- 4. EMPLOYMENT ASSIGNMENT

CREATE TABLE employment_assignment (
    assignment_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    employee_id BIGINT UNSIGNED NOT NULL,
    department_id BIGINT UNSIGNED NOT NULL,
    designation_id BIGINT UNSIGNED NOT NULL,

    reporting_manager_employee_id BIGINT UNSIGNED,

    employment_type VARCHAR(30) NOT NULL,

    effective_from DATE NOT NULL,
    effective_to DATE,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_assignment_employee
        FOREIGN KEY (employee_id)
        REFERENCES employee(employee_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_assignment_department
        FOREIGN KEY (department_id)
        REFERENCES department(department_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_assignment_designation
        FOREIGN KEY (designation_id)
        REFERENCES designation(designation_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_assignment_manager
        FOREIGN KEY (reporting_manager_employee_id)
        REFERENCES employee(employee_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT chk_assignment_dates
        CHECK (
            effective_to IS NULL
            OR effective_to >= effective_from
        ),

    INDEX idx_assignment_employee (employee_id),
    INDEX idx_assignment_department (department_id),
    INDEX idx_assignment_designation (designation_id),
    INDEX idx_assignment_manager (reporting_manager_employee_id),
    INDEX idx_assignment_employee_date
        (employee_id, effective_from)
);

-- 5. EMPLOYMENT STATUS HISTORY

CREATE TABLE employment_status_history (
    status_history_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    employee_id BIGINT UNSIGNED NOT NULL,

    status VARCHAR(30) NOT NULL,

    effective_from DATE NOT NULL,
    effective_to DATE,

    reason VARCHAR(255),

    created_by BIGINT UNSIGNED,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_status_employee
        FOREIGN KEY (employee_id)
        REFERENCES employee(employee_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT chk_status_dates
        CHECK (
            effective_to IS NULL
            OR effective_to >= effective_from
        ),

    INDEX idx_status_employee (employee_id),
    INDEX idx_status_created_by (created_by),
    INDEX idx_status_employee_date
        (employee_id, effective_from)
);

-- =========================================================
-- MODULE 2 : AUTHENTICATION / AUTHORIZATION
-- =========================================================
-- user_account, role, permission, user_role, role_permission

-- 6. USER ACCOUNT

CREATE TABLE user_account (
    user_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    employee_id BIGINT UNSIGNED NOT NULL UNIQUE,

    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    last_login_at DATETIME,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_account_employee
        FOREIGN KEY (employee_id)
        REFERENCES employee(employee_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    INDEX idx_user_account_active (is_active)
);

-- 7. ROLE

CREATE TABLE role (
    role_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    role_code VARCHAR(40) NOT NULL UNIQUE,
    role_name VARCHAR(100) NOT NULL UNIQUE,

    description VARCHAR(255),

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_role_active (is_active)
);

-- 8. PERMISSION

CREATE TABLE permission (
    permission_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    permission_code VARCHAR(100) NOT NULL UNIQUE,
    permission_name VARCHAR(150) NOT NULL,

    resource VARCHAR(60) NOT NULL,
    action VARCHAR(30) NOT NULL,

    description VARCHAR(255),

    INDEX idx_permission_resource_action (resource, action)
);

-- 9. USER ROLE

CREATE TABLE user_role (
    user_id BIGINT UNSIGNED NOT NULL,
    role_id BIGINT UNSIGNED NOT NULL,

    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT UNSIGNED,

    PRIMARY KEY (user_id, role_id),

    CONSTRAINT fk_user_role_user
        FOREIGN KEY (user_id)
        REFERENCES user_account(user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_user_role_role
        FOREIGN KEY (role_id)
        REFERENCES role(role_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_user_role_assigned_by
        FOREIGN KEY (assigned_by)
        REFERENCES user_account(user_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    INDEX idx_user_role_role (role_id),
    INDEX idx_user_role_assigned_by (assigned_by)
);

-- 10. ROLE PERMISSION

CREATE TABLE role_permission (
    role_id BIGINT UNSIGNED NOT NULL,
    permission_id BIGINT UNSIGNED NOT NULL,

    PRIMARY KEY (role_id, permission_id),

    CONSTRAINT fk_role_permission_role
        FOREIGN KEY (role_id)
        REFERENCES role(role_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_role_permission_permission
        FOREIGN KEY (permission_id)
        REFERENCES permission(permission_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    INDEX idx_role_permission_permission (permission_id)
);

-- =========================================================
-- MODULE 7 : ADMIN / CONFIGURATION (salary side)
-- =========================================================
-- salary_component, salary_structure, salary_structure_component,
-- employee_salary

-- 11. SALARY COMPONENT

CREATE TABLE salary_component (
    component_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    component_code VARCHAR(40) NOT NULL UNIQUE,
    component_name VARCHAR(100) NOT NULL,

    component_type VARCHAR(30) NOT NULL,
    calculation_method VARCHAR(30) NOT NULL,

    taxable BOOLEAN NOT NULL DEFAULT FALSE,
    pf_applicable BOOLEAN NOT NULL DEFAULT FALSE,
    esi_applicable BOOLEAN NOT NULL DEFAULT FALSE,

    affects_gross BOOLEAN NOT NULL DEFAULT FALSE,
    affects_net BOOLEAN NOT NULL DEFAULT FALSE,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_salary_component_type
        CHECK (
            component_type IN (
                'EARNING',
                'DEDUCTION',
                'EMPLOYER_CONTRIBUTION'
            )
        ),

    INDEX idx_salary_component_type (component_type),
    INDEX idx_salary_component_active (is_active)
);

-- 12. SALARY STRUCTURE

CREATE TABLE salary_structure (
    salary_structure_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    structure_code VARCHAR(40) NOT NULL UNIQUE,
    structure_name VARCHAR(100) NOT NULL,

    description VARCHAR(500),

    effective_from DATE NOT NULL,
    effective_to DATE,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_salary_structure_dates
        CHECK (
            effective_to IS NULL
            OR effective_to >= effective_from
        ),

    INDEX idx_salary_structure_active (is_active),
    INDEX idx_salary_structure_dates
        (effective_from, effective_to)
);

-- 13. SALARY STRUCTURE COMPONENT

CREATE TABLE salary_structure_component (
    structure_component_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    salary_structure_id BIGINT UNSIGNED NOT NULL,
    component_id BIGINT UNSIGNED NOT NULL,

    calculation_method VARCHAR(30) NOT NULL,

    value DECIMAL(15,4),

    percentage_of_component_id BIGINT UNSIGNED,

    sequence_no INT NOT NULL,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_ssc_structure
        FOREIGN KEY (salary_structure_id)
        REFERENCES salary_structure(salary_structure_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_ssc_component
        FOREIGN KEY (component_id)
        REFERENCES salary_component(component_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_ssc_percentage_component
        FOREIGN KEY (percentage_of_component_id)
        REFERENCES salary_component(component_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT chk_ssc_value
        CHECK (
            value IS NULL
            OR value >= 0
        ),

    CONSTRAINT chk_ssc_sequence
        CHECK (sequence_no > 0),

    CONSTRAINT uq_salary_structure_component
        UNIQUE (salary_structure_id, component_id),

    INDEX idx_ssc_structure (salary_structure_id),
    INDEX idx_ssc_component (component_id),
    INDEX idx_ssc_percentage_component
        (percentage_of_component_id),
    INDEX idx_ssc_sequence
        (salary_structure_id, sequence_no)
);

-- 14. EMPLOYEE SALARY

CREATE TABLE employee_salary (
    employee_salary_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    employee_id BIGINT UNSIGNED NOT NULL,
    salary_structure_id BIGINT UNSIGNED NOT NULL,

    effective_from DATE NOT NULL,
    effective_to DATE,

    annual_ctc DECIMAL(15,2) NOT NULL,
    monthly_gross DECIMAL(15,2) NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_employee_salary_dates
        CHECK (
            effective_to IS NULL
            OR effective_to >= effective_from
        ),

    CONSTRAINT chk_employee_salary_amounts
        CHECK (
            annual_ctc >= 0
            AND monthly_gross >= 0
        ),

    CONSTRAINT fk_employee_salary_employee
        FOREIGN KEY (employee_id)
        REFERENCES employee(employee_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_employee_salary_structure
        FOREIGN KEY (salary_structure_id)
        REFERENCES salary_structure(salary_structure_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    INDEX idx_employee_salary_employee
        (employee_id),

    INDEX idx_employee_salary_structure
        (salary_structure_id),

    INDEX idx_employee_salary_date
        (employee_id, effective_from)
);

-- =========================================================
-- MODULE 7 : ADMIN / CONFIGURATION (leave side)
-- =========================================================
-- leave_type, leave_policy, leave_policy_entitlement,
-- employee_leave_policy

-- 15. LEAVE TYPE

CREATE TABLE leave_type (
    leave_type_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    leave_code VARCHAR(30) NOT NULL UNIQUE,
    leave_name VARCHAR(100) NOT NULL,

    description VARCHAR(255),

    is_paid BOOLEAN NOT NULL DEFAULT TRUE,
    requires_approval BOOLEAN NOT NULL DEFAULT TRUE,
    allows_half_day BOOLEAN NOT NULL DEFAULT FALSE,
    allows_carry_forward BOOLEAN NOT NULL DEFAULT FALSE,

    max_carry_forward DECIMAL(6,2),

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_leave_carry_forward
        CHECK (
            max_carry_forward IS NULL
            OR max_carry_forward >= 0
        ),

    INDEX idx_leave_type_active (is_active)
);

-- 16. LEAVE POLICY

CREATE TABLE leave_policy (
    leave_policy_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    policy_code VARCHAR(40) NOT NULL UNIQUE,
    policy_name VARCHAR(100) NOT NULL,

    description VARCHAR(500),

    effective_from DATE NOT NULL,
    effective_to DATE,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_leave_policy_dates
        CHECK (
            effective_to IS NULL
            OR effective_to >= effective_from
        ),

    INDEX idx_leave_policy_active (is_active),
    INDEX idx_leave_policy_dates
        (effective_from, effective_to)
);

-- 17. LEAVE POLICY ENTITLEMENT

CREATE TABLE leave_policy_entitlement (
    entitlement_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    leave_policy_id BIGINT UNSIGNED NOT NULL,
    leave_type_id BIGINT UNSIGNED NOT NULL,

    annual_entitlement DECIMAL(6,2) NOT NULL,

    accrual_method VARCHAR(30) NOT NULL,
    accrual_frequency VARCHAR(30) NOT NULL,

    carry_forward_allowed BOOLEAN NOT NULL DEFAULT FALSE,

    max_carry_forward DECIMAL(6,2),

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_entitlement_policy
        FOREIGN KEY (leave_policy_id)
        REFERENCES leave_policy(leave_policy_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_entitlement_leave_type
        FOREIGN KEY (leave_type_id)
        REFERENCES leave_type(leave_type_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT chk_entitlement_amount
        CHECK (annual_entitlement >= 0),

    CONSTRAINT chk_entitlement_carry_forward
        CHECK (
            max_carry_forward IS NULL
            OR max_carry_forward >= 0
        ),

    CONSTRAINT uq_policy_leave_type
        UNIQUE (leave_policy_id, leave_type_id),

    INDEX idx_entitlement_policy (leave_policy_id),
    INDEX idx_entitlement_leave_type (leave_type_id)
);

-- 18. EMPLOYEE LEAVE POLICY

CREATE TABLE employee_leave_policy (
    employee_leave_policy_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    employee_id BIGINT UNSIGNED NOT NULL,
    leave_policy_id BIGINT UNSIGNED NOT NULL,

    effective_from DATE NOT NULL,
    effective_to DATE,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_employee_leave_policy_employee
        FOREIGN KEY (employee_id)
        REFERENCES employee(employee_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_employee_leave_policy_policy
        FOREIGN KEY (leave_policy_id)
        REFERENCES leave_policy(leave_policy_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT chk_employee_leave_policy_dates
        CHECK (
            effective_to IS NULL
            OR effective_to >= effective_from
        ),

    INDEX idx_employee_leave_policy_employee
        (employee_id),

    INDEX idx_employee_leave_policy_policy
        (leave_policy_id),

    INDEX idx_employee_leave_policy_date
        (employee_id, effective_from)
);

-- =========================================================
-- MODULE 3 : LEAVE
-- =========================================================
-- leave_balance, leave_application, leave_approval

-- 19. LEAVE BALANCE

CREATE TABLE leave_balance (
    leave_balance_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    employee_id BIGINT UNSIGNED NOT NULL,
    leave_type_id BIGINT UNSIGNED NOT NULL,

    leave_year YEAR NOT NULL,

    opening_balance DECIMAL(6,2) NOT NULL DEFAULT 0,
    accrued DECIMAL(6,2) NOT NULL DEFAULT 0,
    used DECIMAL(6,2) NOT NULL DEFAULT 0,
    adjusted DECIMAL(6,2) NOT NULL DEFAULT 0,
    closing_balance DECIMAL(6,2) NOT NULL DEFAULT 0,

    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_leave_balance_employee
        FOREIGN KEY (employee_id)
        REFERENCES employee(employee_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_leave_balance_leave_type
        FOREIGN KEY (leave_type_id)
        REFERENCES leave_type(leave_type_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT chk_leave_balance_values
        CHECK (
            opening_balance >= 0
            AND accrued >= 0
            AND used >= 0
            AND closing_balance >= 0
        ),

    CONSTRAINT uq_leave_balance
        UNIQUE (employee_id, leave_type_id, leave_year),

    INDEX idx_leave_balance_employee (employee_id),
    INDEX idx_leave_balance_leave_type (leave_type_id)
);

-- 20. LEAVE APPLICATION

CREATE TABLE leave_application (
    leave_application_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    employee_id BIGINT UNSIGNED NOT NULL,
    leave_type_id BIGINT UNSIGNED NOT NULL,

    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_days DECIMAL(6,2) NOT NULL,

    reason VARCHAR(500),

    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

    submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_leave_application_employee
        FOREIGN KEY (employee_id)
        REFERENCES employee(employee_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_leave_application_leave_type
        FOREIGN KEY (leave_type_id)
        REFERENCES leave_type(leave_type_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT chk_leave_application_dates
        CHECK (end_date >= start_date),

    CONSTRAINT chk_leave_application_days
        CHECK (total_days > 0),

    CONSTRAINT chk_leave_application_status
        CHECK (
            status IN (
                'PENDING',
                'APPROVED',
                'REJECTED',
                'CANCELLED'
            )
        ),

    INDEX idx_leave_application_employee (employee_id),
    INDEX idx_leave_application_leave_type (leave_type_id),
    INDEX idx_leave_application_status (status),
    INDEX idx_leave_application_dates (start_date, end_date)
);

-- 21. LEAVE APPROVAL

CREATE TABLE leave_approval (
    approval_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    leave_application_id BIGINT UNSIGNED NOT NULL,
    approver_id BIGINT UNSIGNED NOT NULL,

    approval_level INT NOT NULL,

    action VARCHAR(30) NOT NULL,

    comments VARCHAR(500),

    actioned_at DATETIME,

    CONSTRAINT fk_leave_approval_application
        FOREIGN KEY (leave_application_id)
        REFERENCES leave_application(leave_application_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_leave_approval_approver
        FOREIGN KEY (approver_id)
        REFERENCES employee(employee_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT chk_leave_approval_level
        CHECK (approval_level > 0),

    CONSTRAINT chk_leave_approval_action
        CHECK (
            action IN (
                'APPROVED',
                'REJECTED',
                'PENDING',
                'ESCALATED'
            )
        ),

    CONSTRAINT uq_leave_approval_level
        UNIQUE (leave_application_id, approval_level),

    INDEX idx_leave_approval_application (leave_application_id),
    INDEX idx_leave_approval_approver (approver_id)
);

-- =========================================================
-- MODULE 4 : ATTENDANCE
-- =========================================================
-- attendance, attendance_summary

-- 22. ATTENDANCE

CREATE TABLE attendance (
    attendance_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    employee_id BIGINT UNSIGNED NOT NULL,

    attendance_date DATE NOT NULL,

    status VARCHAR(30) NOT NULL,

    check_in DATETIME,
    check_out DATETIME,

    worked_minutes INT,
    overtime_minutes INT NOT NULL DEFAULT 0,

    source VARCHAR(30) NOT NULL,

    remarks VARCHAR(500),

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_attendance_employee
        FOREIGN KEY (employee_id)
        REFERENCES employee(employee_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT chk_attendance_checkout
        CHECK (
            check_out IS NULL
            OR check_in IS NULL
            OR check_out >= check_in
        ),

    CONSTRAINT chk_attendance_minutes
        CHECK (
            (worked_minutes IS NULL OR worked_minutes >= 0)
            AND overtime_minutes >= 0
        ),

    CONSTRAINT uq_attendance_employee_date
        UNIQUE (employee_id, attendance_date),

    INDEX idx_attendance_employee (employee_id),
    INDEX idx_attendance_date (attendance_date)
);

-- 23. ATTENDANCE SUMMARY

CREATE TABLE attendance_summary (
    summary_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    employee_id BIGINT UNSIGNED NOT NULL,

    summary_month TINYINT UNSIGNED NOT NULL,
    summary_year SMALLINT UNSIGNED NOT NULL,

    working_days DECIMAL(6,2) NOT NULL DEFAULT 0,
    present_days DECIMAL(6,2) NOT NULL DEFAULT 0,
    absent_days DECIMAL(6,2) NOT NULL DEFAULT 0,
    paid_leave_days DECIMAL(6,2) NOT NULL DEFAULT 0,
    unpaid_leave_days DECIMAL(6,2) NOT NULL DEFAULT 0,
    lop_days DECIMAL(6,2) NOT NULL DEFAULT 0,
    overtime_minutes INT NOT NULL DEFAULT 0,

    generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_attendance_summary_employee
        FOREIGN KEY (employee_id)
        REFERENCES employee(employee_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT chk_attendance_summary_month
        CHECK (summary_month BETWEEN 1 AND 12),

    CONSTRAINT uq_attendance_summary
        UNIQUE (employee_id, summary_year, summary_month),

    INDEX idx_attendance_summary_employee (employee_id),
    INDEX idx_attendance_summary_period (summary_year, summary_month)
);

-- =========================================================
-- MODULE 7 : ADMIN / CONFIGURATION (payroll side)
-- =========================================================
-- payroll_rule, payroll_rule_version, tax_slab

-- 24. PAYROLL RULE

CREATE TABLE payroll_rule (
    rule_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    rule_code VARCHAR(50) NOT NULL UNIQUE,
    rule_name VARCHAR(150) NOT NULL,

    rule_type VARCHAR(40) NOT NULL,

    description VARCHAR(500),

    priority INT NOT NULL DEFAULT 0,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_payroll_rule_priority
        CHECK (priority >= 0),

    INDEX idx_payroll_rule_type (rule_type),
    INDEX idx_payroll_rule_priority (priority),
    INDEX idx_payroll_rule_active (is_active)
);

-- 25. PAYROLL RULE VERSION

CREATE TABLE payroll_rule_version (
    rule_version_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    rule_id BIGINT UNSIGNED NOT NULL,

    version_number INT NOT NULL,

    effective_from DATE NOT NULL,
    effective_to DATE,

    definition_json JSON NOT NULL,

    status VARCHAR(30) NOT NULL,

    created_by BIGINT UNSIGNED NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_rule_version_rule
        FOREIGN KEY (rule_id)
        REFERENCES payroll_rule(rule_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_rule_version_created_by
        FOREIGN KEY (created_by)
        REFERENCES user_account(user_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT chk_rule_version_number
        CHECK (version_number > 0),

    CONSTRAINT chk_rule_version_dates
        CHECK (
            effective_to IS NULL
            OR effective_to >= effective_from
        ),

    CONSTRAINT uq_rule_version
        UNIQUE (rule_id, version_number),

    INDEX idx_rule_version_rule (rule_id),
    INDEX idx_rule_version_created_by (created_by),
    INDEX idx_rule_version_effective
        (rule_id, effective_from)
);

-- 26. TAX SLAB

CREATE TABLE tax_slab (
    tax_slab_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    tax_regime VARCHAR(40) NOT NULL,

    financial_year VARCHAR(9) NOT NULL,

    slab_order INT NOT NULL,

    lower_limit DECIMAL(15,2) NOT NULL,
    upper_limit DECIMAL(15,2),

    tax_rate DECIMAL(7,4) NOT NULL,

    fixed_tax DECIMAL(15,2) NOT NULL DEFAULT 0,

    cess_rate DECIMAL(7,4) NOT NULL DEFAULT 0,

    effective_from DATE NOT NULL,
    effective_to DATE,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_tax_slab_order
        CHECK (slab_order > 0),

    CONSTRAINT chk_tax_limits
        CHECK (
            lower_limit >= 0
            AND (
                upper_limit IS NULL
                OR upper_limit > lower_limit
            )
        ),

    CONSTRAINT chk_tax_rates
        CHECK (
            tax_rate >= 0
            AND cess_rate >= 0
        ),

    CONSTRAINT chk_tax_fixed
        CHECK (fixed_tax >= 0),

    CONSTRAINT chk_tax_dates
        CHECK (
            effective_to IS NULL
            OR effective_to >= effective_from
        ),

    CONSTRAINT uq_tax_slab_order
        UNIQUE (
            tax_regime,
            financial_year,
            slab_order
        ),

    INDEX idx_tax_regime_year
        (tax_regime, financial_year),

    INDEX idx_tax_effective
        (effective_from, effective_to)
);

-- =========================================================
-- MODULE 5 : PAYROLL
-- =========================================================
-- payroll_run, employee_payroll, payroll_item

-- 27. PAYROLL RUN

CREATE TABLE payroll_run (
    payroll_run_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    payroll_year SMALLINT UNSIGNED NOT NULL,
    payroll_month TINYINT UNSIGNED NOT NULL,

    run_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,

    started_at DATETIME,
    completed_at DATETIME,

    created_by BIGINT UNSIGNED NOT NULL,
    approved_by BIGINT UNSIGNED,
    approved_at DATETIME,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_payroll_run_created_by
        FOREIGN KEY (created_by)
        REFERENCES user_account(user_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_payroll_run_approved_by
        FOREIGN KEY (approved_by)
        REFERENCES user_account(user_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT chk_payroll_run_month
        CHECK (payroll_month BETWEEN 1 AND 12),

    CONSTRAINT chk_payroll_run_completion
        CHECK (
            completed_at IS NULL
            OR started_at IS NULL
            OR completed_at >= started_at
        ),

    CONSTRAINT uq_payroll_run
        UNIQUE (payroll_year, payroll_month, run_type),

    INDEX idx_payroll_run_status (status),
    INDEX idx_payroll_run_created_by (created_by),
    INDEX idx_payroll_run_approved_by (approved_by)
);

-- 28. EMPLOYEE PAYROLL

CREATE TABLE employee_payroll (
    employee_payroll_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    payroll_run_id BIGINT UNSIGNED NOT NULL,
    employee_id BIGINT UNSIGNED NOT NULL,
    employee_salary_id BIGINT UNSIGNED NOT NULL,

    working_days DECIMAL(6,2) NOT NULL DEFAULT 0,
    present_days DECIMAL(6,2) NOT NULL DEFAULT 0,
    paid_leave_days DECIMAL(6,2) NOT NULL DEFAULT 0,
    unpaid_leave_days DECIMAL(6,2) NOT NULL DEFAULT 0,
    lop_days DECIMAL(6,2) NOT NULL DEFAULT 0,

    gross_before_lop DECIMAL(15,2) NOT NULL DEFAULT 0,
    lop_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_earnings DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_deductions DECIMAL(15,2) NOT NULL DEFAULT 0,
    employer_contribution DECIMAL(15,2) NOT NULL DEFAULT 0,
    net_pay DECIMAL(15,2) NOT NULL DEFAULT 0,

    status VARCHAR(30) NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_employee_payroll_run
        FOREIGN KEY (payroll_run_id)
        REFERENCES payroll_run(payroll_run_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_employee_payroll_employee
        FOREIGN KEY (employee_id)
        REFERENCES employee(employee_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_employee_payroll_salary
        FOREIGN KEY (employee_salary_id)
        REFERENCES employee_salary(employee_salary_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT chk_employee_payroll_amounts
        CHECK (
            gross_before_lop >= 0
            AND lop_amount >= 0
            AND total_earnings >= 0
            AND total_deductions >= 0
            AND employer_contribution >= 0
        ),

    CONSTRAINT uq_employee_payroll
        UNIQUE (payroll_run_id, employee_id),

    INDEX idx_employee_payroll_run (payroll_run_id),
    INDEX idx_employee_payroll_employee (employee_id),
    INDEX idx_employee_payroll_salary (employee_salary_id),
    INDEX idx_employee_payroll_status (status)
);

-- 29. PAYROLL ITEM

CREATE TABLE payroll_item (
    payroll_item_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    employee_payroll_id BIGINT UNSIGNED NOT NULL,
    component_id BIGINT UNSIGNED NOT NULL,
    rule_version_id BIGINT UNSIGNED,

    quantity DECIMAL(12,4),
    rate DECIMAL(12,4),
    percentage DECIMAL(7,4),
    calculation_basis DECIMAL(15,2),

    calculated_amount DECIMAL(15,2) NOT NULL,
    final_amount DECIMAL(15,2) NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payroll_item_employee_payroll
        FOREIGN KEY (employee_payroll_id)
        REFERENCES employee_payroll(employee_payroll_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_payroll_item_component
        FOREIGN KEY (component_id)
        REFERENCES salary_component(component_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_payroll_item_rule_version
        FOREIGN KEY (rule_version_id)
        REFERENCES payroll_rule_version(rule_version_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT uq_payroll_item
        UNIQUE (employee_payroll_id, component_id),

    INDEX idx_payroll_item_employee_payroll (employee_payroll_id),
    INDEX idx_payroll_item_component (component_id),
    INDEX idx_payroll_item_rule_version (rule_version_id)
);

-- =========================================================
-- MODULE 6 : AUDIT / SYSTEM
-- =========================================================
-- audit_log

-- 30. AUDIT LOG

CREATE TABLE audit_log (
    audit_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT UNSIGNED NOT NULL,

    action VARCHAR(30) NOT NULL,

    old_values JSON,
    new_values JSON,

    performed_by BIGINT UNSIGNED,
    performed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    ip_address VARCHAR(45),
    request_id VARCHAR(100),

    CONSTRAINT fk_audit_log_performed_by
        FOREIGN KEY (performed_by)
        REFERENCES user_account(user_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    INDEX idx_audit_log_entity (entity_type, entity_id),
    INDEX idx_audit_log_performed_by (performed_by),
    INDEX idx_audit_log_performed_at (performed_at)
);



