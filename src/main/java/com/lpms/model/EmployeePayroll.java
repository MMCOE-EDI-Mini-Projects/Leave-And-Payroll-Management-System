package com.lpms.model;

import java.math.BigDecimal;

public class EmployeePayroll {
    private Long employeePayrollId;
    private Long payrollRunId;
    private Long employeeId;
    
    private BigDecimal workingDays;
    private BigDecimal presentDays;
    private BigDecimal paidLeaveDays;
    private BigDecimal lopDays;
    
    private BigDecimal grossEarnings;
    private BigDecimal lopDeduction;
    private BigDecimal totalDeductions;
    private BigDecimal netPay;
    
    private String paymentStatus;

    public EmployeePayroll() {}

    public Long getEmployeePayrollId() { return employeePayrollId; }
    public void setEmployeePayrollId(Long employeePayrollId) { this.employeePayrollId = employeePayrollId; }

    public Long getPayrollRunId() { return payrollRunId; }
    public void setPayrollRunId(Long payrollRunId) { this.payrollRunId = payrollRunId; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public BigDecimal getWorkingDays() { return workingDays; }
    public void setWorkingDays(BigDecimal workingDays) { this.workingDays = workingDays; }

    public BigDecimal getPresentDays() { return presentDays; }
    public void setPresentDays(BigDecimal presentDays) { this.presentDays = presentDays; }

    public BigDecimal getPaidLeaveDays() { return paidLeaveDays; }
    public void setPaidLeaveDays(BigDecimal paidLeaveDays) { this.paidLeaveDays = paidLeaveDays; }

    public BigDecimal getLopDays() { return lopDays; }
    public void setLopDays(BigDecimal lopDays) { this.lopDays = lopDays; }

    public BigDecimal getGrossEarnings() { return grossEarnings; }
    public void setGrossEarnings(BigDecimal grossEarnings) { this.grossEarnings = grossEarnings; }

    public BigDecimal getLopDeduction() { return lopDeduction; }
    public void setLopDeduction(BigDecimal lopDeduction) { this.lopDeduction = lopDeduction; }

    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; }

    public BigDecimal getNetPay() { return netPay; }
    public void setNetPay(BigDecimal netPay) { this.netPay = netPay; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
}
