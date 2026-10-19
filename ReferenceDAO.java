package com.lpms.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lpms.util.DBUtil;

import java.sql.*;

/** Lookup lists for account-creation forms. */
public class ReferenceDAO {

    private JsonArray query(String sql, String idCol, String labelCol) throws SQLException {
        JsonArray arr = new JsonArray();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", rs.getLong(idCol));
                o.addProperty("name", rs.getString(labelCol));
                arr.add(o);
            }
        }
        return arr;
    }

    public JsonObject all() throws SQLException {
        JsonObject o = new JsonObject();
        o.add("departments", query("SELECT department_id, department_name FROM department WHERE is_active = TRUE ORDER BY department_name", "department_id", "department_name"));
        o.add("designations", query("SELECT designation_id, designation_name FROM designation WHERE is_active = TRUE ORDER BY designation_name", "designation_id", "designation_name"));
        o.add("managers", query("SELECT employee_id, CONCAT(first_name, ' ', last_name, ' (', employee_code, ')') AS label FROM employee WHERE is_active = TRUE AND role IN ('MANAGER','HR_ADMIN') ORDER BY first_name", "employee_id", "label"));
        return o;
    }
}
