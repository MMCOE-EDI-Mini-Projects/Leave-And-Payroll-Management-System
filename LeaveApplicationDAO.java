package com.lpms.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lpms.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/** Read-side queries for leave applications (writes happen transactionally in LeaveService). */
public class LeaveApplicationDAO {

    private static final String SELECT =
        "SELECT la.leave_application_id, la.employee_id, CONCAT(e.first_name, ' ', e.last_name) AS employee_name, e.employee_code, " +
        "la.leave_type_id, lt.leave_name, la.start_date, la.end_date, la.total_days, la.reason, la.status, " +
        "la.approver_id, CONCAT(a.first_name, ' ', a.last_name) AS approver_name, la.actioned_at, la.manager_comments, la.created_at, " +
        "lb.closing_balance AS balance_remaining " +
        "FROM leave_application la " +
        "JOIN employee e ON e.employee_id = la.employee_id " +
        "JOIN leave_type lt ON lt.leave_type_id = la.leave_type_id " +
        "LEFT JOIN employee a ON a.employee_id = la.approver_id " +
        "LEFT JOIN leave_balance lb ON lb.employee_id = la.employee_id AND lb.leave_type_id = la.leave_type_id AND lb.leave_year = YEAR(la.start_date) ";

    private static String ts(ResultSet rs, String col) throws SQLException {
        Timestamp t = rs.getTimestamp(col);
        return t == null ? null : t.toLocalDateTime().withNano(0).toString().replace('T', ' ');
    }

    private static JsonObject map(ResultSet rs) throws SQLException {
        JsonObject o = new JsonObject();
        o.addProperty("leaveApplicationId", rs.getLong("leave_application_id"));
        o.addProperty("employeeId", rs.getLong("employee_id"));
        o.addProperty("employeeName", rs.getString("employee_name"));
        o.addProperty("employeeCode", rs.getString("employee_code"));
        o.addProperty("leaveTypeId", rs.getLong("leave_type_id"));
        o.addProperty("leaveName", rs.getString("leave_name"));
        o.addProperty("startDate", rs.getDate("start_date").toString());
        o.addProperty("endDate", rs.getDate("end_date").toString());
        o.addProperty("totalDays", rs.getBigDecimal("total_days"));
        o.addProperty("reason", rs.getString("reason"));
        o.addProperty("status", rs.getString("status"));
        long approver = rs.getLong("approver_id");
        if (!rs.wasNull()) {
            o.addProperty("approverId", approver);
            o.addProperty("approverName", rs.getString("approver_name"));
        }
        o.addProperty("actionedAt", ts(rs, "actioned_at"));
        o.addProperty("managerComments", rs.getString("manager_comments"));
        o.addProperty("createdAt", ts(rs, "created_at"));
        o.addProperty("balanceRemaining", rs.getBigDecimal("balance_remaining"));
        return o;
    }

    private JsonArray run(String sql, Object... params) throws SQLException {
        JsonArray out = new JsonArray();
        try (Connection c = DBUtil.getConnection(); PreparedStatement st = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) st.setObject(i + 1, params[i]);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    /** The employee's own applications, newest first. */
    public JsonArray findByEmployee(long employeeId) throws SQLException {
        return run(SELECT + "WHERE la.employee_id = ? ORDER BY la.created_at DESC, la.leave_application_id DESC", employeeId);
    }

    /** Pending requests for a manager's direct reports; HR (allEmployees) sees everyone's. Never includes the viewer's own. */
    public JsonArray findPending(long viewerId, boolean allEmployees) throws SQLException {
        String scope = allEmployees ? "" : "AND e.reporting_manager_id = ? ";
        String sql = SELECT + "WHERE la.status = 'PENDING' AND la.employee_id <> ? " + scope + "ORDER BY la.created_at ASC";
        return allEmployees ? run(sql, viewerId) : run(sql, viewerId, viewerId);
    }

    /** Recently decided requests (approved/rejected/cancelled) in the viewer's scope. */
    public JsonArray findDecided(long viewerId, boolean allEmployees) throws SQLException {
        String scope = allEmployees ? "" : "AND e.reporting_manager_id = ? ";
        String sql = SELECT + "WHERE la.status <> 'PENDING' AND la.employee_id <> ? " + scope +
                     "ORDER BY COALESCE(la.actioned_at, la.updated_at) DESC LIMIT 50";
        return allEmployees ? run(sql, viewerId) : run(sql, viewerId, viewerId);
    }

    /** {employeeId, reportingManagerId or 0}; null if the application does not exist. */
    public long[] owner(long applicationId) throws SQLException {
        String sql = "SELECT la.employee_id, e.reporting_manager_id FROM leave_application la " +
                     "JOIN employee e ON e.employee_id = la.employee_id WHERE la.leave_application_id = ?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement st = c.prepareStatement(sql)) {
            st.setLong(1, applicationId);
            try (ResultSet rs = st.executeQuery()) {
                if (!rs.next()) return null;
                long mgr = rs.getLong(2);
                return new long[]{rs.getLong(1), rs.wasNull() ? 0 : mgr};
            }
        }
    }

    /** Decision history (submitted / approved / rejected / cancelled) of one application, oldest first. */
    public JsonArray history(long applicationId) throws SQLException {
        String sql = "SELECT h.action, h.comments, h.performed_at, CONCAT(p.first_name, ' ', p.last_name) AS performer " +
                     "FROM leave_history h JOIN employee p ON p.employee_id = h.performed_by " +
                     "WHERE h.leave_application_id = ? ORDER BY h.history_id";
        JsonArray out = new JsonArray();
        try (Connection c = DBUtil.getConnection(); PreparedStatement st = c.prepareStatement(sql)) {
            st.setLong(1, applicationId);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    JsonObject o = new JsonObject();
                    o.addProperty("action", rs.getString("action"));
                    o.addProperty("comments", rs.getString("comments"));
                    o.addProperty("performedAt", ts(rs, "performed_at"));
                    o.addProperty("performedBy", rs.getString("performer"));
                    out.add(o);
                }
            }
        }
        return out;
    }

    public JsonArray activeTypes() throws SQLException {
        String sql = "SELECT leave_type_id, leave_code, leave_name, annual_entitlement, is_paid, allows_half_day " +
                     "FROM leave_type WHERE is_active = TRUE ORDER BY leave_name";
        JsonArray out = new JsonArray();
        try (Connection c = DBUtil.getConnection(); PreparedStatement st = c.prepareStatement(sql); ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                JsonObject o = new JsonObject();
                o.addProperty("leaveTypeId", rs.getLong("leave_type_id"));
                o.addProperty("leaveCode", rs.getString("leave_code"));
                o.addProperty("leaveName", rs.getString("leave_name"));
                o.addProperty("annualEntitlement", rs.getBigDecimal("annual_entitlement"));
                o.addProperty("isPaid", rs.getBoolean("is_paid"));
                o.addProperty("allowsHalfDay", rs.getBoolean("allows_half_day"));
                out.add(o);
            }
        }
        return out;
    }
}
