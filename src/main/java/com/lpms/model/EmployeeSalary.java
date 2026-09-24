package com.lpms.model;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class EmployeeSalary {
    private Long employeeSalaryId;
    private Long employeeId;
    private BigDecimal annualCtc;
    private BigDecimal monthlyGross;
    private Date effectiveFrom;
    private Date effectiveTo;
    private boolean isActive;
    
    private List<EmployeeSalaryDetail> details;

    public EmployeeSalary() {}

    public Long getEmployeeSalaryId() { return employeeSalaryId; }
    public void setEmployeeSalaryId(Long employeeSalaryId) { this.employeeSalaryId = employeeSalaryId; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public BigDecimal getAnnualCtc() { return annualCtc; }
    public void setAnnualCtc(BigDecimal annualCtc) { this.annualCtc = annualCtc; }

    public BigDecimal getMonthlyGross() { return monthlyGross; }
    public void setMonthlyGross(BigDecimal monthlyGross) { this.monthlyGross = monthlyGross; }

    public Date getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(Date effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public Date getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(Date effectiveTo) { this.effectiveTo = effectiveTo; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public List<EmployeeSalaryDetail> getDetails() { return details; }
    public void setDetails(List<EmployeeSalaryDetail> details) { this.details = details; }
}
