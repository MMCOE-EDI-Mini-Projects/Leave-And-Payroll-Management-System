package com.lpms.dao;

import com.lpms.model.Employee;
import com.lpms.model.Role;
import com.lpms.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmployeeDAO {

    public Employee findByUsername(String username) {
        String sql = "SELECT * FROM employee WHERE username = ? AND is_active = TRUE";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToEmployee(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Employee findById(Long employeeId) {
        String sql = "SELECT * FROM employee WHERE employee_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, employeeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToEmployee(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Employee> findAll() {
        List<Employee> employees = new ArrayList<>();
        String sql = "SELECT * FROM employee ORDER BY employee_id DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                employees.add(mapRowToEmployee(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return employees;
    }

    /** Used by the auth filter on every request. Returns null if missing or inactive. Throws on DB failure. */
    public Employee findActiveForSession(Long employeeId) throws SQLException {
        String sql = "SELECT * FROM employee WHERE employee_id = ? AND is_active = TRUE";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, employeeId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapRowToEmployee(rs) : null;
            }
        }
    }

    public List<Employee> findByManager(Long managerId) {
        List<Employee> list = new ArrayList<>();
        String sql = "SELECT * FROM employee WHERE reporting_manager_id = ? ORDER BY employee_id DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, managerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRowToEmployee(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Creates an employee account. SQLException propagates so callers can report duplicates (error 1062). */
    public boolean create(Employee employee) throws SQLException {
        String sql = "INSERT INTO employee (employee_code, first_name, last_name, gender, date_of_birth, email, phone, " +
                "date_of_joining, employment_type, employment_status, department_id, designation_id, username, password_hash, role, " +
                "reporting_manager_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, employee.getEmployeeCode());
            stmt.setString(2, employee.getFirstName());
            stmt.setString(3, employee.getLastName());
            stmt.setString(4, employee.getGender());
            setDateOrNull(stmt, 5, employee.getDateOfBirth());
            stmt.setString(6, employee.getEmail());
            stmt.setString(7, employee.getPhone());
            stmt.setDate(8, new java.sql.Date(employee.getDateOfJoining().getTime()));
            stmt.setString(9, employee.getEmploymentType());
            stmt.setString(10, employee.getEmploymentStatus() == null ? "ACTIVE" : employee.getEmploymentStatus());
            stmt.setLong(11, employee.getDepartmentId());
            stmt.setLong(12, employee.getDesignationId());
            stmt.setString(13, employee.getUsername());
            stmt.setString(14, employee.getPasswordHash());
            stmt.setString(15, employee.getRole().name());
            if (employee.getReportingManagerId() == null) stmt.setNull(16, Types.BIGINT);
            else stmt.setLong(16, employee.getReportingManagerId());
            return stmt.executeUpdate() > 0;
        }
    }

    private static void setDateOrNull(PreparedStatement st, int idx, java.util.Date d) throws SQLException {
        if (d == null) st.setNull(idx, Types.DATE); else st.setDate(idx, new java.sql.Date(d.getTime()));
    }

    // ---------------------------------------------------------------- Stage 3: view / search / update / delete

    private static final String DETAIL_SELECT =
        "SELECT e.*, d.department_name, g.designation_name, CONCAT(m.first_name, ' ', m.last_name) AS manager_name " +
        "FROM employee e " +
        "JOIN department d ON d.department_id = e.department_id " +
        "JOIN designation g ON g.designation_id = e.designation_id " +
        "LEFT JOIN employee m ON m.employee_id = e.reporting_manager_id ";

    /** Single employee with department/designation/manager names, or null. */
    public Employee findDetailById(Long id) {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement(DETAIL_SELECT + "WHERE e.employee_id = ?")) {
            st.setLong(1, id);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next() ? mapDetail(rs) : null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Search/filter. All filters are optional (null = ignore). SQL is assembled only from fixed fragments;
     * every user value is bound as a parameter.
     * @param q          matches code, name, email, username or phone (substring, case-insensitive)
     * @param active     null = both, TRUE/FALSE = account active flag
     * @param managerId  restrict to direct reports of this manager (null = everyone)
     */
    public List<Employee> search(String q, Long departmentId, String employmentStatus, Boolean active, Long managerId) throws SQLException {
        StringBuilder sql = new StringBuilder(DETAIL_SELECT).append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            sql.append("AND (e.employee_code LIKE ? OR e.first_name LIKE ? OR e.last_name LIKE ? " +
                       "OR CONCAT(e.first_name, ' ', e.last_name) LIKE ? OR e.email LIKE ? " +
                       "OR e.username LIKE ? OR e.phone LIKE ?) ");
            String like = "%" + q.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            for (int i = 0; i < 7; i++) params.add(like);
        }
        if (departmentId != null) { sql.append("AND e.department_id = ? "); params.add(departmentId); }
        if (employmentStatus != null) { sql.append("AND e.employment_status = ? "); params.add(employmentStatus); }
        if (active != null) { sql.append("AND e.is_active = ? "); params.add(active); }
        if (managerId != null) { sql.append("AND e.reporting_manager_id = ? "); params.add(managerId); }
        sql.append("ORDER BY e.employee_code LIMIT 500");

        List<Employee> out = new ArrayList<>();
        try (Connection c = DBUtil.getConnection(); PreparedStatement st = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) st.setObject(i + 1, params.get(i));
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) out.add(mapDetail(rs));
            }
        }
        return out;
    }

    /**
     * Updates personal/contact data, assignment, joining date and employment status. Employee code, username,
     * password and role are NOT changed here. Keeps is_active consistent with employment_status.
     */
    public boolean updateProfile(Employee e) throws SQLException {
        String sql = "UPDATE employee SET first_name = ?, last_name = ?, gender = ?, date_of_birth = ?, email = ?, phone = ?, " +
                "date_of_joining = ?, date_of_exit = ?, employment_type = ?, employment_status = ?, department_id = ?, " +
                "designation_id = ?, reporting_manager_id = ?, is_active = ? WHERE employee_id = ?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, e.getFirstName());
            st.setString(2, e.getLastName());
            st.setString(3, e.getGender());
            setDateOrNull(st, 4, e.getDateOfBirth());
            st.setString(5, e.getEmail());
            st.setString(6, e.getPhone());
            setDateOrNull(st, 7, e.getDateOfJoining());
            setDateOrNull(st, 8, e.getDateOfExit());
            st.setString(9, e.getEmploymentType());
            st.setString(10, e.getEmploymentStatus());
            st.setLong(11, e.getDepartmentId());
            st.setLong(12, e.getDesignationId());
            if (e.getReportingManagerId() == null) st.setNull(13, Types.BIGINT); else st.setLong(13, e.getReportingManagerId());
            st.setBoolean(14, e.isActive());
            st.setLong(15, e.getEmployeeId());
            return st.executeUpdate() > 0;
        }
    }

    /** Deactivates (RESIGNED/TERMINATED + exit date) or reactivates (ACTIVE, exit date cleared) an account. */
    public boolean setAccountState(Long id, boolean active, String exitStatus) {
        if (active) {
            return update("UPDATE employee SET is_active = TRUE, employment_status = 'ACTIVE', date_of_exit = NULL WHERE employee_id = ?", id);
        }
        return update("UPDATE employee SET is_active = FALSE, employment_status = ?, " +
                "date_of_exit = COALESCE(date_of_exit, GREATEST(CURDATE(), date_of_joining)) WHERE employee_id = ?", exitStatus, id);
    }

    /** True if making {@code managerId} the manager of {@code employeeId} would create a reporting loop. */
    public boolean wouldCreateCycle(Long employeeId, Long managerId) throws SQLException {
        Long cur = managerId;
        for (int depth = 0; cur != null && depth < 100; depth++) {
            if (cur.equals(employeeId)) return true;
            try (Connection c = DBUtil.getConnection();
                 PreparedStatement st = c.prepareStatement("SELECT reporting_manager_id FROM employee WHERE employee_id = ?")) {
                st.setLong(1, cur);
                try (ResultSet rs = st.executeQuery()) {
                    if (!rs.next()) return false;
                    long next = rs.getLong(1);
                    cur = rs.wasNull() ? null : next;
                }
            }
        }
        return cur != null; // depth limit hit: treat as unsafe
    }

    /**
     * Names the kind of history that prevents a hard delete, or null if the employee has none.
     * Deleting would otherwise cascade away attendance or fail on payroll/leave foreign keys.
     */
    public String deleteBlocker(Long id) throws SQLException {
        String[][] checks = {
            {"SELECT COUNT(*) FROM attendance WHERE employee_id = ?", "attendance records"},
            {"SELECT COUNT(*) FROM leave_application WHERE employee_id = ? OR approver_id = ?", "leave applications", "2"},
            {"SELECT COUNT(*) FROM employee_salary WHERE employee_id = ?", "salary records"},
            {"SELECT COUNT(*) FROM employee_payroll WHERE employee_id = ?", "payroll records"},
            {"SELECT COUNT(*) FROM payroll_run WHERE processed_by = ? OR approved_by = ?", "payroll runs", "2"},
            {"SELECT COUNT(*) FROM employee WHERE reporting_manager_id = ?", "direct reports"},
        };
        try (Connection c = DBUtil.getConnection()) {
            for (String[] chk : checks) {
                try (PreparedStatement st = c.prepareStatement(chk[0])) {
                    int n = chk.length > 2 ? Integer.parseInt(chk[2]) : 1;
                    for (int i = 1; i <= n; i++) st.setLong(i, id);
                    try (ResultSet rs = st.executeQuery()) {
                        if (rs.next() && rs.getLong(1) > 0) return chk[1];
                    }
                }
            }
        }
        return null;
    }

    /** Permanent delete. Callers must check {@link #deleteBlocker} first. */
    public boolean delete(Long id) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement st = c.prepareStatement("DELETE FROM employee WHERE employee_id = ?")) {
            st.setLong(1, id);
            return st.executeUpdate() > 0;
        }
    }

    public boolean setActive(Long employeeId, boolean active) {
        return update("UPDATE employee SET is_active = ? WHERE employee_id = ?", active, employeeId);
    }

    public boolean updateRole(Long employeeId, Role role) {
        return update("UPDATE employee SET role = ? WHERE employee_id = ?", role.name(), employeeId);
    }

    public boolean updatePasswordHash(Long employeeId, String hash) {
        return update("UPDATE employee SET password_hash = ? WHERE employee_id = ?", hash, employeeId);
    }

    public void updateLastLogin(Long employeeId) {
        update("UPDATE employee SET last_login_at = CURRENT_TIMESTAMP WHERE employee_id = ?", employeeId);
    }

    public int countActiveHrAdmins() {
        String sql = "SELECT COUNT(*) FROM employee WHERE role = 'HR_ADMIN' AND is_active = TRUE";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private boolean update(String sql, Object... params) {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) stmt.setObject(i + 1, params[i]);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Employee mapRowToEmployee(ResultSet rs) throws SQLException {
        Employee emp = new Employee();
        emp.setEmployeeId(rs.getLong("employee_id"));
        emp.setEmployeeCode(rs.getString("employee_code"));
        emp.setFirstName(rs.getString("first_name"));
        emp.setLastName(rs.getString("last_name"));
        emp.setGender(rs.getString("gender"));
        emp.setDateOfBirth(rs.getDate("date_of_birth"));
        emp.setEmail(rs.getString("email"));
        emp.setPhone(rs.getString("phone"));
        emp.setDateOfJoining(rs.getDate("date_of_joining"));
        emp.setDateOfExit(rs.getDate("date_of_exit"));
        emp.setEmploymentType(rs.getString("employment_type"));
        emp.setEmploymentStatus(rs.getString("employment_status"));
        emp.setDepartmentId(rs.getLong("department_id"));
        emp.setDesignationId(rs.getLong("designation_id"));
        long mgr = rs.getLong("reporting_manager_id");
        emp.setReportingManagerId(rs.wasNull() ? null : mgr);
        emp.setUsername(rs.getString("username"));
        emp.setPasswordHash(rs.getString("password_hash"));
        emp.setRole(Role.valueOf(rs.getString("role")));
        emp.setActive(rs.getBoolean("is_active"));
        emp.setLastLoginAt(rs.getTimestamp("last_login_at"));
        return emp;
    }

    private Employee mapDetail(ResultSet rs) throws SQLException {
        Employee emp = mapRowToEmployee(rs);
        emp.setDepartmentName(rs.getString("department_name"));
        emp.setDesignationName(rs.getString("designation_name"));
        emp.setManagerName(rs.getString("manager_name"));
        return emp;
    }
}
