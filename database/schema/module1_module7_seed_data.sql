USE lpms;


-- 1. DEPARTMENTS


INSERT INTO department
(department_code, department_name, description)
VALUES
('HR',   'Human Resources', 'Human resources and employee administration'),
('FIN',  'Finance', 'Finance, payroll and accounting'),
('ENG',  'Engineering', 'Software and technology development'),
('IT',   'Information Technology', 'IT infrastructure and technical support'),
('MKT',  'Marketing', 'Marketing and business promotion'),
('OPS',  'Operations', 'Business and operational activities');


-- 2. DESIGNATIONS


INSERT INTO designation
(designation_code, designation_name, job_level, description)
VALUES
('CEO',   'Chief Executive Officer', 'L5', 'Organization head'),
('HRM',   'HR Manager', 'L4', 'Human resource management'),
('FINM',  'Finance Manager', 'L4', 'Finance and payroll management'),
('SE',    'Software Engineer', 'L2', 'Software development'),
('SSE',   'Senior Software Engineer', 'L3', 'Senior software development'),
('TL',    'Technical Lead', 'L4', 'Technical team leadership'),
('DA',    'Data Analyst', 'L2', 'Data analysis and reporting'),
('SYS',   'System Administrator', 'L2', 'IT infrastructure management'),
('EXEC',  'HR Executive', 'L1', 'HR operations'),
('INT',   'Software Engineering Intern', 'L0', 'Engineering internship');



-- 3. EMPLOYEES
  

INSERT INTO employee
(
    employee_code,
    first_name,
    middle_name,
    last_name,
    date_of_birth,
    gender,
    email,
    phone,
    date_of_joining,
    employment_status
)
VALUES
(
    'EMP001',
    'Aarav',
    NULL,
    'Sharma',
    '1985-04-12',
    'Male',
    'aarav.sharma@lpms.com',
    '9876500001',
    '2018-06-01',
    'ACTIVE'
),
(
    'EMP002',
    'Priya',
    NULL,
    'Patil',
    '1989-08-20',
    'Female',
    'priya.patil@lpms.com',
    '9876500002',
    '2019-03-15',
    'ACTIVE'
),
(
    'EMP003',
    'Rahul',
    NULL,
    'Deshmukh',
    '1995-02-10',
    'Male',
    'rahul.deshmukh@lpms.com',
    '9876500003',
    '2021-07-12',
    'ACTIVE'
),
(
    'EMP004',
    'Ananya',
    NULL,
    'Kulkarni',
    '1997-11-03',
    'Female',
    'ananya.kulkarni@lpms.com',
    '9876500004',
    '2022-01-10',
    'ACTIVE'
),
(
    'EMP005',
    'Vikram',
    NULL,
    'Joshi',
    '1993-05-17',
    'Male',
    'vikram.joshi@lpms.com',
    '9876500005',
    '2020-09-01',
    'ACTIVE'
),
(
    'EMP006',
    'Sneha',
    NULL,
    'Mehta',
    '1998-06-22',
    'Female',
    'sneha.mehta@lpms.com',
    '9876500006',
    '2023-06-05',
    'ACTIVE'
);


  
-- 4. EMPLOYMENT ASSIGNMENTS
  

INSERT INTO employment_assignment
(
    employee_id,
    department_id,
    designation_id,
    reporting_manager_employee_id,
    employment_type,
    effective_from
)
VALUES
-- Aarav - HR Manager
(1, 1, 2, 1, 'FULL_TIME', '2018-06-01'),

-- Priya - Finance Manager
(2, 2, 3, 1, 'FULL_TIME', '2019-03-15'),

-- Rahul - Senior Software Engineer
(3, 3, 5, 1, 'FULL_TIME', '2021-07-12'),

-- Ananya - Software Engineer reporting to Rahul
(4, 3, 4, 3, 'FULL_TIME', '2022-01-10'),

-- Vikram - Data Analyst
(5, 3, 7, 3, 'FULL_TIME', '2020-09-01'),

-- Sneha - Software Engineering Intern
(6, 3, 10, 3, 'INTERN', '2023-06-05');


  
-- 5. EMPLOYMENT STATUS HISTORY
  

INSERT INTO employment_status_history
(
    employee_id,
    status,
    effective_from,
    reason
)
VALUES
(1, 'ACTIVE', '2018-06-01', 'Joined organization'),
(2, 'ACTIVE', '2019-03-15', 'Joined organization'),
(3, 'ACTIVE', '2021-07-12', 'Joined organization'),
(4, 'ACTIVE', '2022-01-10', 'Joined organization'),
(5, 'ACTIVE', '2020-09-01', 'Joined organization'),
(6, 'ACTIVE', '2023-06-05', 'Joined organization');


  
-- 6. SALARY COMPONENTS
  

INSERT INTO salary_component
(
    component_code,
    component_name,
    component_type,
    calculation_method,
    taxable,
    pf_applicable,
    esi_applicable,
    affects_gross,
    affects_net
)
VALUES

('BASIC',
 'Basic Salary',
 'EARNING',
 'PERCENTAGE',
 TRUE,
 TRUE,
 TRUE,
 TRUE,
 TRUE),

('HRA',
 'House Rent Allowance',
 'EARNING',
 'PERCENTAGE',
 TRUE,
 FALSE,
 FALSE,
 TRUE,
 TRUE),

('CONV',
 'Conveyance Allowance',
 'EARNING',
 'FIXED',
 TRUE,
 FALSE,
 FALSE,
 TRUE,
 TRUE),

('MED',
 'Medical Allowance',
 'EARNING',
 'FIXED',
 TRUE,
 FALSE,
 FALSE,
 TRUE,
 TRUE),

('BONUS',
 'Performance Bonus',
 'EARNING',
 'PERCENTAGE',
 TRUE,
 FALSE,
 FALSE,
 TRUE,
 TRUE),

('PF',
 'Provident Fund',
 'DEDUCTION',
 'PERCENTAGE',
 FALSE,
 FALSE,
 FALSE,
 FALSE,
 TRUE),

('PT',
 'Professional Tax',
 'DEDUCTION',
 'FIXED',
 FALSE,
 FALSE,
 FALSE,
 FALSE,
 TRUE),

('TDS',
 'Income Tax / TDS',
 'DEDUCTION',
 'RULE',
 TRUE,
 FALSE,
 FALSE,
 FALSE,
 TRUE),

('ESI',
 'Employee State Insurance',
 'DEDUCTION',
 'PERCENTAGE',
 FALSE,
 FALSE,
 TRUE,
 FALSE,
 TRUE),

('EMP_PF',
 'Employer PF Contribution',
 'EMPLOYER_CONTRIBUTION',
 'PERCENTAGE',
 FALSE,
 FALSE,
 FALSE,
 FALSE,
 FALSE);


  
-- 7. SALARY STRUCTURES
  

INSERT INTO salary_structure
(
    structure_code,
    structure_name,
    description,
    effective_from
)
VALUES
(
    'STD_ENG',
    'Standard Engineering Structure',
    'Salary structure for engineering employees',
    '2026-04-01'
),
(
    'STD_MGMT',
    'Standard Management Structure',
    'Salary structure for managers',
    '2026-04-01'
),
(
    'STD_INTERN',
    'Internship Structure',
    'Salary structure for interns',
    '2026-04-01'
);


  
-- 8. SALARY STRUCTURE COMPONENTS
  

INSERT INTO salary_structure_component
(
    salary_structure_id,
    component_id,
    calculation_method,
    value,
    percentage_of_component_id,
    sequence_no
)
VALUES

-- Engineering Structure
(1, 1, 'PERCENTAGE', 40.0000, NULL, 1),
(1, 2, 'PERCENTAGE', 40.0000, 1,    2),
(1, 3, 'FIXED',      2000.0000, NULL, 3),
(1, 4, 'FIXED',      1500.0000, NULL, 4),
(1, 6, 'PERCENTAGE', 12.0000, 1, 5),
(1, 7, 'FIXED',       200.0000, NULL, 6),
(1, 8, 'RULE',        NULL,     NULL, 7),

-- Management Structure
(2, 1, 'PERCENTAGE', 50.0000, NULL, 1),
(2, 2, 'PERCENTAGE', 40.0000, 1,    2),
(2, 3, 'FIXED',      5000.0000, NULL, 3),
(2, 5, 'PERCENTAGE', 10.0000, 1, 4),
(2, 6, 'PERCENTAGE', 12.0000, 1, 5),
(2, 7, 'FIXED',       200.0000, NULL, 6),
(2, 8, 'RULE',        NULL, NULL, 7),

-- Intern Structure
(3, 3, 'FIXED', 5000.0000, NULL, 1);


  
-- 9. EMPLOYEE SALARY
  

INSERT INTO employee_salary
(
    employee_id,
    salary_structure_id,
    effective_from,
    annual_ctc,
    monthly_gross
)
VALUES
(1, 2, '2026-04-01', 1800000.00, 150000.00),
(2, 2, '2026-04-01', 1500000.00, 125000.00),
(3, 1, '2026-04-01', 1200000.00, 100000.00),
(4, 1, '2026-04-01', 720000.00,  60000.00),
(5, 1, '2026-04-01', 900000.00,  75000.00),
(6, 3, '2026-04-01', 60000.00,    5000.00);


  
-- 10. LEAVE TYPES
  

INSERT INTO leave_type
(
    leave_code,
    leave_name,
    description,
    is_paid,
    requires_approval,
    allows_half_day,
    allows_carry_forward,
    max_carry_forward
)
VALUES
(
    'CL',
    'Casual Leave',
    'Leave for personal or unforeseen short-term requirements',
    TRUE,
    TRUE,
    TRUE,
    FALSE,
    0
),
(
    'SL',
    'Sick Leave',
    'Leave due to illness or medical reasons',
    TRUE,
    TRUE,
    TRUE,
    FALSE,
    0
),
(
    'PL',
    'Privilege Leave',
    'Planned annual leave',
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    15
),
(
    'LWP',
    'Leave Without Pay',
    'Unpaid leave',
    FALSE,
    TRUE,
    FALSE,
    FALSE,
    0
);


  
-- 11. LEAVE POLICIES
  

INSERT INTO leave_policy
(
    policy_code,
    policy_name,
    description,
    effective_from
)
VALUES
(
    'STD_2026',
    'Standard Leave Policy 2026',
    'Standard annual leave policy for full-time employees',
    '2026-04-01'
),
(
    'INTERN_2026',
    'Intern Leave Policy 2026',
    'Leave policy for interns',
    '2026-04-01'
);


  
-- 12. LEAVE POLICY ENTITLEMENTS
  

INSERT INTO leave_policy_entitlement
(
    leave_policy_id,
    leave_type_id,
    annual_entitlement,
    accrual_method,
    accrual_frequency,
    carry_forward_allowed,
    max_carry_forward
)
VALUES

-- Standard Policy
(1, 1, 12.00, 'MONTHLY', 'MONTHLY', FALSE, 0),
(1, 2, 12.00, 'MONTHLY', 'MONTHLY', FALSE, 0),
(1, 3, 15.00, 'ANNUAL',  'YEARLY',  TRUE, 15),
(1, 4,  0.00, 'NONE',    'NONE',    FALSE, 0),

-- Intern Policy
(2, 1,  6.00, 'MONTHLY', 'MONTHLY', FALSE, 0),
(2, 2,  6.00, 'MONTHLY', 'MONTHLY', FALSE, 0),
(2, 4,  0.00, 'NONE',    'NONE',    FALSE, 0);


  
-- 13. EMPLOYEE LEAVE POLICY
  

INSERT INTO employee_leave_policy
(
    employee_id,
    leave_policy_id,
    effective_from
)
VALUES
(1, 1, '2026-04-01'),
(2, 1, '2026-04-01'),
(3, 1, '2026-04-01'),
(4, 1, '2026-04-01'),
(5, 1, '2026-04-01'),
(6, 2, '2026-04-01');


  
-- 14. PAYROLL RULES
  

INSERT INTO payroll_rule
(
    rule_code,
    rule_name,
    rule_type,
    description,
    priority
)
VALUES
(
    'PF_RULE',
    'Provident Fund Calculation',
    'DEDUCTION',
    'Calculates employee provident fund contribution',
    10
),
(
    'ESI_RULE',
    'ESI Calculation',
    'DEDUCTION',
    'Calculates employee ESI contribution',
    20
),
(
    'LOP_RULE',
    'Loss of Pay Calculation',
    'LOP',
    'Calculates salary deduction for unpaid absence',
    30
),
(
    'TDS_RULE',
    'Income Tax Calculation',
    'TAX',
    'Calculates income tax based on applicable tax rules',
    40
);


  
-- 15. PAYROLL RULE VERSIONS
  

INSERT INTO payroll_rule_version
(
    rule_id,
    version_number,
    effective_from,
    definition_json,
    status,
    created_by
)
VALUES

(
    1,
    1,
    '2026-04-01',
    JSON_OBJECT(
        'base', 'BASIC',
        'rate', 12,
        'maximum_limit', 15000,
        'description', 'Employee PF contribution'
    ),
    'ACTIVE',
    1
),

(
    2,
    1,
    '2026-04-01',
    JSON_OBJECT(
        'threshold', 21000,
        'employee_rate', 0.75,
        'description', 'Employee ESI contribution'
    ),
    'ACTIVE',
    1
),

(
    3,
    1,
    '2026-04-01',
    JSON_OBJECT(
        'formula', 'monthly_gross / working_days * lop_days',
        'description', 'Loss of Pay deduction'
    ),
    'ACTIVE',
    1
),

(
    4,
    1,
    '2026-04-01',
    JSON_OBJECT(
        'method', 'TAX_SLAB',
        'regime', 'NEW',
        'description', 'Income tax calculation'
    ),
    'ACTIVE',
    1
);


  
-- 16. TAX SLABS
  

INSERT INTO tax_slab
(
    tax_regime,
    financial_year,
    slab_order,
    lower_limit,
    upper_limit,
    tax_rate,
    fixed_tax,
    cess_rate,
    effective_from
)
VALUES
(
    'NEW',
    '2026-27',
    1,
    0,
    400000,
    0.0000,
    0,
    0.0000,
    '2026-04-01'
),
(
    'NEW',
    '2026-27',
    2,
    400000,
    800000,
    0.0500,
    0,
    0.0400,
    '2026-04-01'
),
(
    'NEW',
    '2026-27',
    3,
    800000,
    1200000,
    0.1000,
    20000,
    0.0400,
    '2026-04-01'
),
(
    'NEW',
    '2026-27',
    4,
    1200000,
    1600000,
    0.1500,
    60000,
    0.0400,
    '2026-04-01'
),
(
    'NEW',
    '2026-27',
    5,
    1600000,
    2000000,
    0.2000,
    120000,
    0.0400,
    '2026-04-01'
),
(
    'NEW',
    '2026-27',
    6,
    2000000,
    2400000,
    0.2500,
    200000,
    0.0400,
    '2026-04-01'
),
(
    'NEW',
    '2026-27',
    7,
    2400000,
    NULL,
    0.3000,
    300000,
    0.0400,
    '2026-04-01'
);
