package com.lpms.service;

import com.lpms.dao.LeaveApplicationDAO;
import com.lpms.dao.LeaveBalanceDAO;
import com.lpms.model.LeaveApplication;
import com.lpms.util.DBUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Calendar;

public class LeaveService {
    
    private final LeaveApplicationDAO leaveApplicationDAO;
    private final LeaveBalanceDAO leaveBalanceDAO;

    public LeaveService() {
        this.leaveApplicationDAO = new LeaveApplicationDAO();
        this.leaveBalanceDAO = new LeaveBalanceDAO();
    }

    /**
     * Approves a leave application and safely updates the leave balance using a database transaction.
     * 
     * @param application The Leave Application being approved
     * @param approverId The ID of the manager approving
     * @param comments Optional manager comments
     * @return true if successful
     */
    public boolean approveLeave(LeaveApplication application, Long approverId, String comments) {
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false); // Start Transaction

            // 1. Update Leave Application Status
            String sqlApp = "UPDATE leave_application SET status = 'APPROVED', approver_id = ?, actioned_at = CURRENT_TIMESTAMP, manager_comments = ? WHERE leave_application_id = ?";
            try (PreparedStatement stmtApp = conn.prepareStatement(sqlApp)) {
                stmtApp.setLong(1, approverId);
                stmtApp.setString(2, comments);
                stmtApp.setLong(3, application.getLeaveApplicationId());
                if (stmtApp.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }
            }

            // Extract Year for Balance
            Calendar cal = Calendar.getInstance();
            cal.setTime(application.getStartDate());
            int year = cal.get(Calendar.YEAR);

            // 2. Update Leave Balance
            boolean balanceUpdated = leaveBalanceDAO.updateUsedBalance(conn, application.getEmployeeId(), application.getLeaveTypeId(), year, application.getTotalDays());
            
            if (!balanceUpdated) {
                // If there's no balance record for this year, rollback
                conn.rollback();
                return false;
            }

            conn.commit(); // End Transaction (Success)
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    DBUtil.closeConnection(conn);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
