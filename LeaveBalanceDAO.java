package com.lpms.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lpms.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LeaveBalanceDAO {

    /** Creates missing balance rows for the year (entitlement accrued up-front) for every active paid leave type. Idempotent. */
    public static void ensure(Connection c, long employeeId, int year) throws SQLException {
        String sql = "INSERT IGNORE INTO leave_balance (employee_id, leave_type_id, leave_year, opening_balance, accrued, used) " +
                     "SELECT ?, leave_type_id, ?, 0, annual_entitlement, 0 FROM leave_type WHERE is_active = TRUE AND is_paid = TRUE";
        try (PreparedStatement st = c.prepareStatement(sql)) {
            st.setLong(1, employeeId);
            st.setInt(2, year);
            st.executeUpdate();
        }
    }

    /** Entitlement, used, remaining and pending days per leave type for one employee and year. */
    public JsonArray balances(long employeeId, int year) throws SQLException {
        String sql = "SELECT lt.leave_type_id, lt.leave_code, lt.leave_name, lb.opening_balance, lb.accrued, lb.used, lb.closing_balance, " +
                     "COALESCE((SELECT SUM(la.total_days) FROM leave_application la WHERE la.employee_id = lb.employee_id " +
                     "AND la.leave_type_id = lb.leave_type_id AND la.status = 'PENDING' AND YEAR(la.start_date) = lb.leave_year), 0) AS pending " +
                     "FROM leave_balance lb JOIN leave_type lt ON lt.leave_type_id = lb.leave_type_id " +
                     "WHERE lb.employee_id = ? AND lb.leave_year = ? AND lt.is_active = TRUE ORDER BY lt.leave_name";
        JsonArray out = new JsonArray();
        try (Connection c = DBUtil.getConnection()) {
            ensure(c, employeeId, year);
            try (PreparedStatement st = c.prepareStatement(sql)) {
                st.setLong(1, employeeId);
                st.setInt(2, year);
                try (ResultSet rs = st.executeQuery()) {
                    while (rs.next()) {
                        JsonObject o = new JsonObject();
                        o.addProperty("leaveTypeId", rs.getLong("leave_type_id"));
                        o.addProperty("leaveCode", rs.getString("leave_code"));
                        o.addProperty("leaveName", rs.getString("leave_name"));
                        o.addProperty("year", year);
                        o.addProperty("opening", rs.getBigDecimal("opening_balance"));
                        o.addProperty("entitlement", rs.getBigDecimal("accrued"));
                        o.addProperty("used", rs.getBigDecimal("used"));
                        o.addProperty("remaining", rs.getBigDecimal("closing_balance"));
                        o.addProperty("pending", rs.getBigDecimal("pending"));
                        o.addProperty("available", rs.getBigDecimal("closing_balance").subtract(rs.getBigDecimal("pending")));
                        out.add(o);
                    }
                }
            }
        }
        return out;
    }
}
