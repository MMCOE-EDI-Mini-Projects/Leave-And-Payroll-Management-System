package com.lpms.model;

import java.util.Date;
import java.sql.Timestamp;

public class Attendance {
    private Long attendanceId;
    private Long employeeId;
    private Date attendanceDate;
    private Timestamp checkIn;
    private Timestamp checkOut;
    private String status;
    private Integer workedMinutes;
    private Integer overtimeMinutes;
    private String remarks;
    private boolean isLocked;

    public Attendance() {}

    public Long getAttendanceId() { return attendanceId; }
    public void setAttendanceId(Long attendanceId) { this.attendanceId = attendanceId; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public Date getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(Date attendanceDate) { this.attendanceDate = attendanceDate; }

    public Timestamp getCheckIn() { return checkIn; }
    public void setCheckIn(Timestamp checkIn) { this.checkIn = checkIn; }

    public Timestamp getCheckOut() { return checkOut; }
    public void setCheckOut(Timestamp checkOut) { this.checkOut = checkOut; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getWorkedMinutes() { return workedMinutes; }
    public void setWorkedMinutes(Integer workedMinutes) { this.workedMinutes = workedMinutes; }

    public Integer getOvertimeMinutes() { return overtimeMinutes; }
    public void setOvertimeMinutes(Integer overtimeMinutes) { this.overtimeMinutes = overtimeMinutes; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public boolean isLocked() { return isLocked; }
    public void setLocked(boolean locked) { isLocked = locked; }
}
