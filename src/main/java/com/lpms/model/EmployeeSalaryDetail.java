package com.lpms.model;

import java.math.BigDecimal;

public class EmployeeSalaryDetail {
    private Long salaryDetailId;
    private Long employeeSalaryId;
    private Long componentId;
    private BigDecimal componentValue;
    
    // Transient field for linking to the actual component in memory
    private SalaryComponent component;

    public EmployeeSalaryDetail() {}

    public Long getSalaryDetailId() { return salaryDetailId; }
    public void setSalaryDetailId(Long salaryDetailId) { this.salaryDetailId = salaryDetailId; }

    public Long getEmployeeSalaryId() { return employeeSalaryId; }
    public void setEmployeeSalaryId(Long employeeSalaryId) { this.employeeSalaryId = employeeSalaryId; }

    public Long getComponentId() { return componentId; }
    public void setComponentId(Long componentId) { this.componentId = componentId; }

    public BigDecimal getComponentValue() { return componentValue; }
    public void setComponentValue(BigDecimal componentValue) { this.componentValue = componentValue; }

    public SalaryComponent getComponent() { return component; }
    public void setComponent(SalaryComponent component) { this.component = component; }
}
