package com.lpms.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lpms.service.AttendanceException;
import com.lpms.service.AttendanceService;
import com.lpms.util.Json;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Attendance API (authentication/CSRF enforced by AuthenticationFilter; ownership/team rules in AttendanceService).
 * GET  /api/attendance/today | my?month= | history?employeeId=&month= | summary?employeeId=&month= |
 *      team-summary?month= (manager/HR) | policy | holidays?year=
 * POST /api/attendance/  body {action: CHECK_IN|CHECK_OUT|RECORD|UPDATE|IMPORT|LOCK|UNLOCK|SAVE_POLICY|ADD_HOLIDAY|DELETE_HOLIDAY, ...}
 */
@WebServlet("/api/attendance/*")
public class AttendanceServlet extends HttpServlet {
    private final AttendanceService service = new AttendanceService();

    private static Long parseLong(String s) {
        try { return s == null ? null : Long.valueOf(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private static LocalDate parseDate(String s) {
        try { return s == null ? null : LocalDate.parse(s); } catch (DateTimeParseException e) { return null; }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long me = (Long) req.getSession().getAttribute("employeeId");
        String role = (String) req.getSession().getAttribute("role");
        String path = req.getPathInfo() == null ? "/" : req.getPathInfo();
        try {
            switch (path) {
                case "/policy":
                    Json.write(resp, 200, service.policyJson());
                    return;
                case "/holidays": {
                    int year = LocalDate.now().getYear();
                    Long y = parseLong(req.getParameter("year"));
                    if (req.getParameter("year") != null && (y == null || y < 2000 || y > year + 2)) { Json.error(resp, 400, "Invalid year."); return; }
                    if (y != null) year = y.intValue();
                    Json.writeArray(resp, 200, service.holidays(year));
                    return;
                }
                case "/today": {
                    JsonArray a = service.summaries(YearMonth.now(), me, null, true);
                    JsonObject out = new JsonObject();
                    String today = LocalDate.now().toString();
                    if (a.size() > 0) for (JsonElement d : a.get(0).getAsJsonObject().getAsJsonArray("days"))
                        if (today.equals(d.getAsJsonObject().get("date").getAsString())) out = d.getAsJsonObject();
                    Json.write(resp, 200, out);
                    return;
                }
                case "/team-summary": {
                    if (!"MANAGER".equals(role) && !"HR_ADMIN".equals(role)) { Json.error(resp, 403, "Only managers and HR can view team attendance."); return; }
                    YearMonth m = AttendanceService.parseMonth(req.getParameter("month"), LocalDate.now());
                    Json.writeArray(resp, 200, service.summaries(m, null, "HR_ADMIN".equals(role) ? null : me, false));
                    return;
                }
                case "/my":
                case "/history":
                case "/summary":
                case "/": {
                    YearMonth m = AttendanceService.parseMonth(req.getParameter("month"), LocalDate.now());
                    Long target = path.equals("/my") || path.equals("/") ? Long.valueOf(me) : parseLong(req.getParameter("employeeId"));
                    if (target == null) {
                        if (req.getParameter("employeeId") != null) { Json.error(resp, 400, "Invalid employeeId."); return; }
                        target = me;
                    }
                    service.requireViewable(me, role, target);
                    JsonArray a = service.summaries(m, target, null, !path.equals("/summary"));
                    if (a.size() == 0) { Json.error(resp, 404, "Employee was not employed in that month."); return; }
                    Json.write(resp, 200, a.get(0).getAsJsonObject());
                    return;
                }
                default:
                    Json.error(resp, 404, "Unknown attendance endpoint.");
            }
        } catch (AttendanceException e) {
            Json.error(resp, e.getStatus(), e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            Json.error(resp, 500, "Failed to load attendance data.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long me = (Long) req.getSession().getAttribute("employeeId");
        String role = (String) req.getSession().getAttribute("role");
        JsonObject body = Json.readBody(req);
        if (body == null) { Json.error(resp, 400, "Invalid JSON body"); return; }
        String a = Json.str(body, "action");
        String action = a == null ? "CHECK_IN" : a.toUpperCase();
        try {
            switch (action) {
                case "CHECK_IN":
                    service.checkIn(me);
                    Json.message(resp, 201, "Checked in.");
                    break;
                case "CHECK_OUT":
                    service.checkOut(me);
                    Json.message(resp, 200, "Checked out.");
                    break;
                case "RECORD": {
                    Long emp = parseLong(Json.str(body, "employeeId"));
                    if (emp == null) { Json.error(resp, 400, "employeeId is required."); return; }
                    String st = Json.str(body, "status");
                    service.record(me, role, emp, parseDate(Json.str(body, "date")), st == null ? null : st.toUpperCase(),
                            Json.str(body, "checkIn"), Json.str(body, "checkOut"), Json.str(body, "remarks"));
                    Json.message(resp, 201, "Attendance recorded.");
                    break;
                }
                case "UPDATE": {
                    Long id = parseLong(Json.str(body, "attendanceId"));
                    if (id == null) { Json.error(resp, 400, "attendanceId is required."); return; }
                    String st = Json.str(body, "status");
                    service.update(me, role, id, st == null ? null : st.toUpperCase(),
                            Json.str(body, "checkIn"), Json.str(body, "checkOut"), Json.str(body, "remarks"));
                    Json.message(resp, 200, "Attendance updated.");
                    break;
                }
                case "IMPORT": {
                    if (!body.has("rows") || !body.get("rows").isJsonArray()) { Json.error(resp, 400, "rows array is required."); return; }
                    Json.write(resp, 200, service.importRows(me, role, body.getAsJsonArray("rows")));
                    break;
                }
                case "LOCK":
                case "UNLOCK": {
                    YearMonth m = AttendanceService.parseMonth(Json.str(body, "month"), LocalDate.now());
                    service.lockMonth(me, role, m, "LOCK".equals(action));
                    Json.message(resp, 200, m + ("LOCK".equals(action) ? " locked." : " unlocked."));
                    break;
                }
                case "SAVE_POLICY": {
                    Map<String, String> changes = new LinkedHashMap<>();
                    for (String k : body.keySet()) if (!k.equals("action")) changes.put(k, Json.str(body, k));
                    if (changes.isEmpty()) { Json.error(resp, 400, "No policy values supplied."); return; }
                    service.savePolicy(me, role, changes);
                    Json.message(resp, 200, "Attendance policy saved.");
                    break;
                }
                case "ADD_HOLIDAY":
                    service.addHoliday(me, role, parseDate(Json.str(body, "date")), Json.str(body, "name"));
                    Json.message(resp, 201, "Holiday added.");
                    break;
                case "DELETE_HOLIDAY": {
                    Long id = parseLong(Json.str(body, "holidayId"));
                    if (id == null) { Json.error(resp, 400, "holidayId is required."); return; }
                    service.deleteHoliday(me, role, id);
                    Json.message(resp, 200, "Holiday removed.");
                    break;
                }
                default:
                    Json.error(resp, 400, "Unknown action.");
            }
        } catch (AttendanceException e) {
            Json.error(resp, e.getStatus(), e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            Json.error(resp, 500, "Attendance operation failed.");
        }
    }
}
