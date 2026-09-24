package com.lpms.dao;

import com.lpms.model.LeaveApplication;
import com.lpms.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LeaveApplicationDAO {

    public boolean create(LeaveApplication leaveApp) {
        String sql = "INSERT INTO leave_application (employee_id, leave_type_id, start_date, end_date, total_days, reason, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, leaveApp.getEmployeeId());
            stmt.setLong(2, leaveApp.getLeaveTypeId());
            stmt.setDate(3, new java.sql.Date(leaveApp.getStartDate().getTime()));
            stmt.setDate(4, new java.sql.Date(leaveApp.getEndDate().getTime()));
            stmt.setBigDecimal(5, leaveApp.getTotalDays());
            stmt.setString(6, leaveApp.getReason());
            stmt.setString(7, "PENDING");

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateStatus(Long leaveApplicationId, String status, Long approverId, String managerComments) {
        String sql = "UPDATE leave_application SET status = ?, approver_id = ?, actioned_at = CURRENT_TIMESTAMP, manager_comments = ? " +
                     "WHERE leave_application_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status);
            stmt.setLong(2, approverId);
            stmt.setString(3, managerComments);
            stmt.setLong(4, leaveApplicationId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<LeaveApplication> findByEmployeeId(Long employeeId) {
        List<LeaveApplication> leaves = new ArrayList<>();
        String sql = "SELECT * FROM leave_application WHERE employee_id = ? ORDER BY created_at DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, employeeId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    leaves.add(mapRowToLeaveApplication(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return leaves;
    }
    
    public List<LeaveApplication> findPendingForManager(Long managerId) {
        List<LeaveApplication> leaves = new ArrayList<>();
        String sql = "SELECT la.* FROM leave_application la " +
                     "JOIN employee e ON la.employee_id = e.employee_id " +
                     "WHERE e.reporting_manager_id = ? AND la.status = 'PENDING' " +
                     "ORDER BY la.created_at ASC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, managerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    leaves.add(mapRowToLeaveApplication(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return leaves;
    }

    private LeaveApplication mapRowToLeaveApplication(ResultSet rs) throws SQLException {
        LeaveApplication la = new LeaveApplication();
        la.setLeaveApplicationId(rs.getLong("leave_application_id"));
        la.setEmployeeId(rs.getLong("employee_id"));
        la.setLeaveTypeId(rs.getLong("leave_type_id"));
        la.setStartDate(rs.getDate("start_date"));
        la.setEndDate(rs.getDate("end_date"));
        la.setTotalDays(rs.getBigDecimal("total_days"));
        la.setReason(rs.getString("reason"));
        la.setStatus(rs.getString("status"));
        la.setApproverId(rs.getLong("approver_id"));
        la.setActionedAt(rs.getTimestamp("actioned_at"));
        la.setManagerComments(rs.getString("manager_comments"));
        la.setCreatedAt(rs.getTimestamp("created_at"));
        return la;
    }
}
