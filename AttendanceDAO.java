package com.lpms.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lpms.util.AttendanceRules;
import com.lpms.util.DBUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** JDBC access for attendance, holidays, policy and month locks. All queries are parameterised. */
public class AttendanceDAO {

    public record Emp(long id, String code, String name, LocalDate joined, LocalDate exited, Long managerId, boolean active) {}
    public record Row(long id, long employeeId, LocalDate date, String status, LocalDateTime checkIn, LocalDateTime checkOut,
                      int workedMinutes, int overtimeMinutes, String remarks, boolean locked) {}
    /** Result of the SQL GROUP BY aggregation over recorded rows for one employee-month. */
    public record Agg(int present, int half, int absent, long worked, long overtime) {}

    private static final String EMP_SELECT =
        "SELECT employee_id, employee_code, CONCAT(first_name,' ',last_name) AS name, date_of_joining, date_of_exit, " +
        "reporting_manager_id, is_active FROM employee ";

    private static Emp emp(ResultSet rs) throws SQLException {
        Date ex = rs.getDate("date_of_exit");
        long m = rs.getLong("reporting_manager_id");
        Long mgr = rs.wasNull() ? null : m;
        return new Emp(rs.getLong("employee_id"), rs.getString("employee_code"), rs.getString("name"),
                rs.getDate("date_of_joining").toLocalDate(), ex == null ? null : ex.toLocalDate(), mgr, rs.getBoolean("is_active"));
    }

    private static void bind(PreparedStatement st, Object... p) throws SQLException {
        for (int i = 0; i < p.length; i++) st.setObject(i + 1, p[i]);
    }

    public Emp employeeById(long id) throws SQLException {
        try (Connection c = DBUtil.getConnection(); PreparedStatement st = c.prepareStatement(EMP_SELECT + "WHERE employee_id = ?")) {
            st.setLong(1, id);
            try (ResultSet rs = st.executeQuery()) { return rs.next() ? emp(rs) : null; }
        }
    }

    public Emp employeeByCode(String code) throws SQLException {
        try (Connection c = DBUtil.getConnection(); PreparedStatement st = c.prepareStatement(EMP_SELECT + "WHERE employee_code = ?")) {
            st.setString(1, code);
            try (ResultSet rs = st.executeQuery()) { return rs.next() ? emp(rs) : null; }
        }
    }

    /** Employees employed at any point in the month, optionally only one employee or one manager's direct reports. */
    public List<Emp> employeesForMonth(YearMonth m, Long employeeId, Long managerId) throws SQLException {
        StringBuilder sql = new StringBuilder(EMP_SELECT + "WHERE date_of_joining <= ? AND (date_of_exit IS NULL OR date_of_exit >= ?) ");
        List<Object> p = new ArrayList<>(List.of(Date.valueOf(m.atEndOfMonth()), Date.valueOf(m.atDay(1))));
        if (employeeId != null) { sql.append("AND employee_id = ? "); p.add(employeeId); }
        if (managerId != null) { sql.append("AND reporting_manager_id = ? "); p.add(managerId); }
        sql.append("ORDER BY employee_code");
        List<Emp> out = new ArrayList<>();
        try (Connection c = DBUtil.getConnection(); PreparedStatement st = c.prepareStatement(sql.toString())) {
            bind(st, p.toArray());
            try (ResultSet rs = st.executeQuery()) { while (rs.next()) out.add(emp(rs)); }
        }
        return out;
    }

    private static Row row(ResultSet rs) throws SQLException {
        Timestamp in = rs.getTimestamp("check_in"), out = rs.getTimestamp("check_out");
        return new Row(rs.getLong("attendance_id"), rs.getLong("employee_id"), rs.getDate("attendance_date").toLocalDate(),
                rs.getString("status"), in == null ? null : in.toLocalDateTime(), out == null ? null : out.toLocalDateTime(),
                rs.getInt("worked_minutes"), rs.getInt("overtime_minutes"), rs.getString("remarks"), rs.getBoolean("is_locked"));
    }

    public Row findById(long id) throws SQLException {
        try (Connection c = DBUtil.getConnection(); PreparedStatement st = c.prepareStatement("SELECT * FROM attendance WHERE attendance_id = ?")) {
            st.setLong(1, id);
            try (ResultSet rs = st.executeQuery()) { return rs.next() ? row(rs) : null; }
        }
    }

    public Row findByEmployeeDate(long employeeId, LocalDate d) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement("SELECT * FROM attendance WHERE employee_id = ? AND attendance_date = ?")) {
            st.setLong(1, employeeId);
            st.setDate(2, Date.valueOf(d));
            try (ResultSet rs = st.executeQuery()) { return rs.next() ? row(rs) : null; }
        }
    }

    /** Attendance rows for the month grouped by employee (range query, uses idx_att_emp_date). */
    public Map<Long, Map<LocalDate, Row>> rowsForMonth(YearMonth m, Long employeeId, Long managerId) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT a.* FROM attendance a JOIN employee e ON e.employee_id = a.employee_id " +
                "WHERE a.attendance_date BETWEEN ? AND ? ");
        List<Object> p = new ArrayList<>(List.of(Date.valueOf(m.atDay(1)), Date.valueOf(m.atEndOfMonth())));
        if (employeeId != null) { sql.append("AND a.employee_id = ? "); p.add(employeeId); }
        if (managerId != null) { sql.append("AND e.reporting_manager_id = ? "); p.add(managerId); }
        Map<Long, Map<LocalDate, Row>> out = new HashMap<>();
        try (Connection c = DBUtil.getConnection(); PreparedStatement st = c.prepareStatement(sql.toString())) {
            bind(st, p.toArray());
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) { Row r = row(rs); out.computeIfAbsent(r.employeeId(), k -> new HashMap<>()).put(r.date(), r); }
            }
        }
        return out;
    }

    /** SQL aggregation: recorded status counts, worked and overtime minutes per employee for the month. */
    public Map<Long, Agg> aggregate(YearMonth m, Long employeeId, Long managerId) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT a.employee_id, SUM(a.status = 'PRESENT') AS present_cnt, SUM(a.status = 'HALF_DAY') AS half_cnt, " +
            "SUM(a.status = 'ABSENT') AS absent_cnt, COALESCE(SUM(a.worked_minutes),0) AS worked, COALESCE(SUM(a.overtime_minutes),0) AS ot " +
            "FROM attendance a JOIN employee e ON e.employee_id = a.employee_id WHERE a.attendance_date BETWEEN ? AND ? ");
        List<Object> p = new ArrayList<>(List.of(Date.valueOf(m.atDay(1)), Date.valueOf(m.atEndOfMonth())));
        if (employeeId != null) { sql.append("AND a.employee_id = ? "); p.add(employeeId); }
        if (managerId != null) { sql.append("AND e.reporting_manager_id = ? "); p.add(managerId); }
        sql.append("GROUP BY a.employee_id");
        Map<Long, Agg> out = new HashMap<>();
        try (Connection c = DBUtil.getConnection(); PreparedStatement st = c.prepareStatement(sql.toString())) {
            bind(st, p.toArray());
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) out.put(rs.getLong("employee_id"), new Agg(rs.getInt("present_cnt"), rs.getInt("half_cnt"),
                        rs.getInt("absent_cnt"), rs.getLong("worked"), rs.getLong("ot")));
            }
        }
        return out;
    }

    /** Approved leave expanded to working days (Mon-Fri, not holidays) per employee for the month. */
    public Map<Long, Map<LocalDate, AttendanceRules.Leave>> approvedLeaves(YearMonth m, Long employeeId, Long managerId,
                                                                            Set<LocalDate> holidays) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT la.employee_id, la.start_date, la.end_date, la.total_days, lt.is_paid, lt.leave_name " +
            "FROM leave_application la JOIN leave_type lt ON lt.leave_type_id = la.leave_type_id " +
            "JOIN employee e ON e.employee_id = la.employee_id " +
            "WHERE la.status = 'APPROVED' AND la.start_date <= ? AND la.end_date >= ? ");
        List<Object> p = new ArrayList<>(List.of(Date.valueOf(m.atEndOfMonth()), Date.valueOf(m.atDay(1))));
        if (employeeId != null) { sql.append("AND la.employee_id = ? "); p.add(employeeId); }
        if (managerId != null) { sql.append("AND e.reporting_manager_id = ? "); p.add(managerId); }
        Map<Long, Map<LocalDate, AttendanceRules.Leave>> out = new HashMap<>();
        try (Connection c = DBUtil.getConnection(); PreparedStatement st = c.prepareStatement(sql.toString())) {
            bind(st, p.toArray());
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    LocalDate s = rs.getDate("start_date").toLocalDate(), e = rs.getDate("end_date").toLocalDate();
                    boolean half = s.equals(e) && rs.getBigDecimal("total_days").compareTo(AttendanceRules.HALF) == 0;
                    BigDecimal frac = half ? AttendanceRules.HALF : BigDecimal.ONE;
                    Map<LocalDate, AttendanceRules.Leave> days = out.computeIfAbsent(rs.getLong("employee_id"), k -> new HashMap<>());
                    for (LocalDate d = s.isBefore(m.atDay(1)) ? m.atDay(1) : s; !d.isAfter(e) && !d.isAfter(m.atEndOfMonth()); d = d.plusDays(1)) {
                        if (AttendanceRules.isWeekend(d) || holidays.contains(d)) continue;
                        days.merge(d, new AttendanceRules.Leave(frac, rs.getBoolean("is_paid"), rs.getString("leave_name")),
                                (a, b) -> new AttendanceRules.Leave(a.fraction().add(b.fraction()).min(BigDecimal.ONE), a.paid(), a.name()));
                    }
                }
            }
        }
        return out;
    }

    // ---- writes ----

    /** Inserts a record; a duplicate (employee, date) surfaces as SQLException with error code 1062. */
    public long insert(long employeeId, LocalDate d, String status, LocalDateTime in, LocalDateTime out,
                       int worked, int overtime, String remarks) throws SQLException {
        String sql = "INSERT INTO attendance (employee_id, attendance_date, check_in, check_out, status, worked_minutes, overtime_minutes, remarks, is_locked) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, FALSE)";
        try (Connection c = DBUtil.getConnection(); PreparedStatement st = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            st.setLong(1, employeeId);
            st.setDate(2, Date.valueOf(d));
            st.setTimestamp(3, in == null ? null : Timestamp.valueOf(in));
            st.setTimestamp(4, out == null ? null : Timestamp.valueOf(out));
            st.setString(5, status);
            st.setInt(6, worked);
            st.setInt(7, overtime);
            st.setString(8, remarks);
            st.executeUpdate();
            try (ResultSet k = st.getGeneratedKeys()) { return k.next() ? k.getLong(1) : 0; }
        }
    }

    public boolean update(long id, String status, LocalDateTime in, LocalDateTime out, int worked, int overtime, String remarks) throws SQLException {
        String sql = "UPDATE attendance SET status = ?, check_in = ?, check_out = ?, worked_minutes = ?, overtime_minutes = ?, remarks = ? " +
                     "WHERE attendance_id = ? AND is_locked = FALSE";
        try (Connection c = DBUtil.getConnection(); PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, status);
            st.setTimestamp(2, in == null ? null : Timestamp.valueOf(in));
            st.setTimestamp(3, out == null ? null : Timestamp.valueOf(out));
            st.setInt(4, worked);
            st.setInt(5, overtime);
            st.setString(6, remarks);
            st.setLong(7, id);
            return st.executeUpdate() > 0;
        }
    }

    // ---- month lock (stored in attendance_policy as LOCK:YYYY-MM so empty months can be locked too) ----

    public boolean isMonthLocked(YearMonth m) throws SQLException {
        try (Connection c = DBUtil.getConnection(); PreparedStatement st = c.prepareStatement("SELECT 1 FROM attendance_policy WHERE policy_key = ?")) {
            st.setString(1, "LOCK:" + m);
            try (ResultSet rs = st.executeQuery()) { return rs.next(); }
        }
    }

    public void setMonthLocked(YearMonth m, boolean lock, long by) throws SQLException {
        try (Connection c = DBUtil.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement st = c.prepareStatement(lock
                        ? "INSERT IGNORE INTO attendance_policy (policy_key, policy_value, description, updated_by) VALUES (?, 'Y', 'Month locked', ?)"
                        : "DELETE FROM attendance_policy WHERE policy_key = ?")) {
                    st.setString(1, "LOCK:" + m);
                    if (lock) st.setLong(2, by);
                    st.executeUpdate();
                }
                try (PreparedStatement st = c.prepareStatement("UPDATE attendance SET is_locked = ? WHERE attendance_date BETWEEN ? AND ?")) {
                    st.setBoolean(1, lock);
                    st.setDate(2, Date.valueOf(m.atDay(1)));
                    st.setDate(3, Date.valueOf(m.atEndOfMonth()));
                    st.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    // ---- policy ----

    public Map<String, String> policyValues() throws SQLException {
        Map<String, String> out = new LinkedHashMap<>();
        try (Connection c = DBUtil.getConnection(); PreparedStatement st = c.prepareStatement("SELECT policy_key, policy_value FROM attendance_policy");
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) out.put(rs.getString(1), rs.getString(2));
        }
        return out;
    }

    public void savePolicy(String key, String value, long by) throws SQLException {
        String sql = "INSERT INTO attendance_policy (policy_key, policy_value, updated_by) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE policy_value = VALUES(policy_value), updated_by = VALUES(updated_by)";
        try (Connection c = DBUtil.getConnection(); PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, key);
            st.setString(2, value);
            st.setLong(3, by);
            st.executeUpdate();
        }
    }

    // ---- holidays ----

    public Set<LocalDate> holidaySet(LocalDate from, LocalDate to) throws SQLException {
        Set<LocalDate> out = new HashSet<>();
        try (Connection c = DBUtil.getConnection(); PreparedStatement st = c.prepareStatement("SELECT holiday_date FROM holiday WHERE holiday_date BETWEEN ? AND ?")) {
            st.setDate(1, Date.valueOf(from));
            st.setDate(2, Date.valueOf(to));
            try (ResultSet rs = st.executeQuery()) { while (rs.next()) out.add(rs.getDate(1).toLocalDate()); }
        }
        return out;
    }

    public JsonArray holidays(int year) throws SQLException {
        JsonArray out = new JsonArray();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement("SELECT holiday_id, holiday_date, holiday_name FROM holiday WHERE YEAR(holiday_date) = ? ORDER BY holiday_date")) {
            st.setInt(1, year);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    JsonObject o = new JsonObject();
                    o.addProperty("holidayId", rs.getLong(1));
                    o.addProperty("date", rs.getDate(2).toString());
                    o.addProperty("name", rs.getString(3));
                    out.add(o);
                }
            }
        }
        return out;
    }

    public boolean addHoliday(LocalDate d, String name, long by) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement("INSERT IGNORE INTO holiday (holiday_date, holiday_name, created_by) VALUES (?, ?, ?)")) {
            st.setDate(1, Date.valueOf(d));
            st.setString(2, name);
            st.setLong(3, by);
            return st.executeUpdate() > 0;
        }
    }

    public boolean deleteHoliday(long id) throws SQLException {
        try (Connection c = DBUtil.getConnection(); PreparedStatement st = c.prepareStatement("DELETE FROM holiday WHERE holiday_id = ?")) {
            st.setLong(1, id);
            return st.executeUpdate() > 0;
        }
    }
}
