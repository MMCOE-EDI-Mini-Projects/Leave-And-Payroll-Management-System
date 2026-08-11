-- =========================================================
-- LEAVE AND PAYROLL MANAGEMENT SYSTEM
-- DATABASE SCHEMA
-- =========================================================

-- Module 1: Employee Management
-- Module 7: Admin / Configuration

CREATE DATABASE IF NOT EXISTS lpms;

USE lpms;

-- =========================================================
-- MODULE 1 : EMPLOYEE MANAGEMENT
-- =========================================================

-- department
-- designation
-- employee
-- employment_assignment
-- employment_status_history
-- employee_salary

-- CREATE TABLE statements...


-- =========================================================
-- MODULE 7 : ADMIN / CONFIGURATION
-- =========================================================

-- salary_component
-- salary_structure
-- salary_structure_component
-- leave_type
-- leave_policy
-- leave_policy_entitlement
-- employee_leave_policy
-- payroll_rule
-- payroll_rule_version
-- tax_slab


-- Module 1 Employee Management
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
        (employee_id, effective_from)-- 5. EMPLOYMENT STATUS HISTORY
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

-- MODULE 7 : ADMIN / CONFIGURATION

-- 6. SALARY COMPONENT

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

-- 7. SALARY STRUCTURE

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

-- 8. SALARY STRUCTURE COMPONENT

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

-- 9. EMPLOYEE SALARY

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

-- LEAVE CONFIGURATION
-- 10. LEAVE TYPE

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

-- 11. LEAVE POLICY

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

-- 12. LEAVE POLICY ENTITLEMENT

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

-- 13. EMPLOYEE LEAVE POLICY

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

-- PAYROLL CONFIGURATION
-- 14. PAYROLL RULE

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

-- 15. PAYROLL RULE VERSION

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

-- 16. TAX SLAB

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

-- END OF MODULE 1 + MODULE 7 SCHEMA

