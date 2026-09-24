package com.lpms.model;

import java.math.BigDecimal;
import java.util.Date;

public class LeaveType {
    private Long leaveTypeId;
    private String leaveCode;
    private String leaveName;
    private String description;
    private BigDecimal annualEntitlement;
    private boolean isPaid;
    private boolean allowsHalfDay;
    private boolean allowsCarryForward;
    private BigDecimal maxCarryForward;
    private boolean isActive;
    
    // Default constructor
    public LeaveType() {}

    // Getters and Setters
    public Long getLeaveTypeId() { return leaveTypeId; }
    public void setLeaveTypeId(Long leaveTypeId) { this.leaveTypeId = leaveTypeId; }

    public String getLeaveCode() { return leaveCode; }
    public void setLeaveCode(String leaveCode) { this.leaveCode = leaveCode; }

    public String getLeaveName() { return leaveName; }
    public void setLeaveName(String leaveName) { this.leaveName = leaveName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getAnnualEntitlement() { return annualEntitlement; }
    public void setAnnualEntitlement(BigDecimal annualEntitlement) { this.annualEntitlement = annualEntitlement; }

    public boolean isPaid() { return isPaid; }
    public void setPaid(boolean paid) { isPaid = paid; }

    public boolean isAllowsHalfDay() { return allowsHalfDay; }
    public void setAllowsHalfDay(boolean allowsHalfDay) { this.allowsHalfDay = allowsHalfDay; }

    public boolean isAllowsCarryForward() { return allowsCarryForward; }
    public void setAllowsCarryForward(boolean allowsCarryForward) { this.allowsCarryForward = allowsCarryForward; }

    public BigDecimal getMaxCarryForward() { return maxCarryForward; }
    public void setMaxCarryForward(BigDecimal maxCarryForward) { this.maxCarryForward = maxCarryForward; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
