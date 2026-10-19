package com.lpms.dao;

import com.lpms.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/** Best-effort writer for the audit_log table; an audit failure never blocks the business operation. */
public class AuditDAO {
    public void log(String entity, Long entityId, String action, Long performedBy, String oldJson, String newJson) {
        String sql = "INSERT INTO audit_log (entity_name, entity_id, action, performed_by, old_values, new_values) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = DBUtil.getConnection(); PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, entity);
            st.setLong(2, entityId);
            st.setString(3, action);
            if (performedBy == null) st.setNull(4, Types.BIGINT); else st.setLong(4, performedBy);
            if (oldJson == null) st.setNull(5, Types.VARCHAR); else st.setString(5, oldJson);
            if (newJson == null) st.setNull(6, Types.VARCHAR); else st.setString(6, newJson);
            st.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public com.google.gson.JsonArray findRecent(int limit, String entity) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT a.audit_id, a.entity_name, a.entity_id, a.action, a.performed_by, " +
            "CONCAT(e.first_name, ' ', e.last_name) AS performer_name, a.performed_at, " +
            "a.old_values, a.new_values " +
            "FROM audit_log a " +
            "LEFT JOIN employee e ON e.employee_id = a.performed_by "
        );
        if (entity != null && !entity.isBlank()) {
            sql.append("WHERE a.entity_name = ? ");
        }
        sql.append("ORDER BY a.audit_id DESC LIMIT ?");

        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement(sql.toString())) {
            int paramIdx = 1;
            if (entity != null && !entity.isBlank()) {
                st.setString(paramIdx++, entity.trim());
            }
            st.setInt(paramIdx, limit > 0 ? limit : 50);

            try (java.sql.ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    com.google.gson.JsonObject o = new com.google.gson.JsonObject();
                    o.addProperty("auditId", rs.getLong("audit_id"));
                    o.addProperty("entityName", rs.getString("entity_name"));
                    o.addProperty("entityId", rs.getLong("entity_id"));
                    o.addProperty("action", rs.getString("action"));
                    long pb = rs.getLong("performed_by");
                    if (!rs.wasNull()) {
                        o.addProperty("performedBy", pb);
                        o.addProperty("performerName", rs.getString("performer_name"));
                    }
                    if (rs.getTimestamp("performed_at") != null) {
                        o.addProperty("performedAt", rs.getTimestamp("performed_at").toString());
                    }
                    o.addProperty("oldValues", rs.getString("old_values"));
                    o.addProperty("newValues", rs.getString("new_values"));
                    arr.add(o);
                }
            }
        }
        return arr;
    }
}
