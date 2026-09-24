package com.lpms.model;

import java.math.BigDecimal;
import java.util.Date;

public class LeaveApplication {
    private Long leaveApplicationId;
    private Long employeeId;
    private Long leaveTypeId;
    private Date startDate;
    private Date endDate;
    private BigDecimal totalDays;
    private String reason;
    private String status;
    private Long approverId;
    private Date actionedAt;
    private String managerComments;
    private Date createdAt;

    public LeaveApplication() {}

    public Long getLeaveApplicationId() { return leaveApplicationId; }
    public void setLeaveApplicationId(Long leaveApplicationId) { this.leaveApplicationId = leaveApplicationId; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public Long getLeaveTypeId() { return leaveTypeId; }
    public void setLeaveTypeId(Long leaveTypeId) { this.leaveTypeId = leaveTypeId; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public BigDecimal getTotalDays() { return totalDays; }
    public void setTotalDays(BigDecimal totalDays) { this.totalDays = totalDays; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getApproverId() { return approverId; }
    public void setApproverId(Long approverId) { this.approverId = approverId; }

    public Date getActionedAt() { return actionedAt; }
    public void setActionedAt(Date actionedAt) { this.actionedAt = actionedAt; }

    public String getManagerComments() { return managerComments; }
    public void setManagerComments(String managerComments) { this.managerComments = managerComments; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
