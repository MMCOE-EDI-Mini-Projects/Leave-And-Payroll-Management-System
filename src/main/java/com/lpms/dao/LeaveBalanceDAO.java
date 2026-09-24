package com.lpms.dao;

import com.lpms.util.DBUtil;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class LeaveBalanceDAO {

    /**
     * Updates the used leave balance for an employee within a specific transaction.
     * 
     * @param conn Transactional connection
     * @param employeeId ID of the employee
     * @param leaveTypeId ID of the leave type
     * @param year The leave year
     * @param daysToDeduct The number of days to add to the 'used' balance
     * @return true if successful
     * @throws SQLException if database error occurs
     */
    public boolean updateUsedBalance(Connection conn, Long employeeId, Long leaveTypeId, int year, BigDecimal daysToDeduct) throws SQLException {
        String sql = "UPDATE leave_balance SET used = used + ? WHERE employee_id = ? AND leave_type_id = ? AND leave_year = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, daysToDeduct);
            stmt.setLong(2, employeeId);
            stmt.setLong(3, leaveTypeId);
            stmt.setInt(4, year);
            
            return stmt.executeUpdate() > 0;
        }
    }
}
