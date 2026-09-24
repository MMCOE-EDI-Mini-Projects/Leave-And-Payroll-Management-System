package com.lpms.dao;

import com.lpms.model.Attendance;
import com.lpms.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AttendanceDAO {

    public boolean create(Attendance attendance) {
        String sql = "INSERT INTO attendance (employee_id, attendance_date, check_in, check_out, status, remarks, is_locked) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, attendance.getEmployeeId());
            stmt.setDate(2, new java.sql.Date(attendance.getAttendanceDate().getTime()));
            stmt.setTimestamp(3, attendance.getCheckIn());
            stmt.setTimestamp(4, attendance.getCheckOut());
            stmt.setString(5, attendance.getStatus());
            stmt.setString(6, attendance.getRemarks());
            stmt.setBoolean(7, false); // By default, not locked

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Attendance> findByEmployeeId(Long employeeId, String yearMonth) {
        List<Attendance> attendances = new ArrayList<>();
        // Search for a specific month e.g., '2026-09'
        String sql = "SELECT * FROM attendance WHERE employee_id = ? AND DATE_FORMAT(attendance_date, '%Y-%m') = ? ORDER BY attendance_date DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, employeeId);
            stmt.setString(2, yearMonth);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    attendances.add(mapRowToAttendance(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return attendances;
    }

    private Attendance mapRowToAttendance(ResultSet rs) throws SQLException {
        Attendance a = new Attendance();
        a.setAttendanceId(rs.getLong("attendance_id"));
        a.setEmployeeId(rs.getLong("employee_id"));
        a.setAttendanceDate(rs.getDate("attendance_date"));
        a.setCheckIn(rs.getTimestamp("check_in"));
        a.setCheckOut(rs.getTimestamp("check_out"));
        a.setStatus(rs.getString("status"));
        a.setWorkedMinutes(rs.getInt("worked_minutes"));
        a.setOvertimeMinutes(rs.getInt("overtime_minutes"));
        a.setRemarks(rs.getString("remarks"));
        a.setLocked(rs.getBoolean("is_locked"));
        return a;
    }
}
