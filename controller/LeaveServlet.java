package com.lpms.controller;

import com.lpms.dao.LeaveApplicationDAO;
import com.lpms.dao.LeaveBalanceDAO;
import com.lpms.service.LeaveApprovalQueue;
import com.lpms.service.LeaveException;
import com.lpms.service.LeaveService;
import com.lpms.util.Json;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Leave API (authentication/CSRF enforced by AuthenticationFilter; ownership and team rules enforced here and in LeaveService).
 * GET  /api/leaves/types | balances?year= | my | pending | decided | {id}/history
 * POST /api/leaves/*  body {action: APPLY|APPROVE|REJECT|CANCEL, ...}
 */
@WebServlet("/api/leaves/*")
public class LeaveServlet extends HttpServlet {
    private static final Pattern HISTORY = Pattern.compile("^/(\\d+)/history$");

    private final LeaveApplicationDAO applications = new LeaveApplicationDAO();
    private final LeaveBalanceDAO balances = new LeaveBalanceDAO();
    private final LeaveService service = new LeaveService();

    private static boolean isStaff(String role) { return "MANAGER".equals(role) || "HR_ADMIN".equals(role); }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long me = (Long) req.getSession().getAttribute("employeeId");
        String role = (String) req.getSession().getAttribute("role");
        String path = req.getPathInfo() == null ? "/" : req.getPathInfo();
        try {
            Matcher h = HISTORY.matcher(path);
            if (path.equals("/types")) {
                Json.writeArray(resp, 200, applications.activeTypes());
            } else if (path.equals("/balances")) {
                int year = LocalDate.now().getYear();
                String y = req.getParameter("year");
                if (y != null && !y.isBlank()) {
                    try { year = Integer.parseInt(y.trim()); } catch (NumberFormatException e) { Json.error(resp, 400, "Invalid year."); return; }
                    int now = LocalDate.now().getYear();
                    if (year < now - 1 || year > now + 1) { Json.error(resp, 400, "Year must be within one year of the current year."); return; }
                }
                Json.writeArray(resp, 200, balances.balances(me, year));
            } else if (path.equals("/my") || path.equals("/")) {
                Json.writeArray(resp, 200, applications.findByEmployee(me));
            } else if (path.equals("/pending") || path.equals("/decided")) {
                if (!isStaff(role)) { Json.error(resp, 403, "Only managers can view team leave requests."); return; }
                boolean all = "HR_ADMIN".equals(role);
                Json.writeArray(resp, 200, path.equals("/pending") ? applications.findPending(me, all) : applications.findDecided(me, all));
            } else if (path.equals("/queue") || path.equals("/queue/next")) {
                if (!isStaff(role)) { Json.error(resp, 403, "Only managers can access the approval queue."); return; }
                boolean all = "HR_ADMIN".equals(role);
                com.google.gson.JsonArray pendingArr = applications.findPending(me, all);
                java.util.List<JsonObject> list = new java.util.ArrayList<>();
                for (int i = 0; i < pendingArr.size(); i++) {
                    list.add(pendingArr.get(i).getAsJsonObject());
                }
                LeaveApprovalQueue approvalQueue = new LeaveApprovalQueue(list);
                if (path.equals("/queue/next")) {
                    JsonObject next = approvalQueue.peekNext();
                    if (next == null) {
                        Json.message(resp, 200, "Approval queue is currently empty.");
                    } else {
                        Json.write(resp, 200, next);
                    }
                } else {
                    Json.writeArray(resp, 200, pendingArr);
                }
            } else if (h.matches()) {
                long id = Long.parseLong(h.group(1));
                long[] owner = applications.owner(id);
                if (owner == null) { Json.error(resp, 404, "Leave application not found."); return; }
                boolean allowed = owner[0] == me || "HR_ADMIN".equals(role) || (isStaff(role) && owner[1] == me);
                if (!allowed) { Json.error(resp, 403, "You are not allowed to view this request."); return; }
                Json.writeArray(resp, 200, applications.history(id));
            } else {
                Json.error(resp, 404, "Unknown leave endpoint.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Json.error(resp, 500, "Failed to load leave data.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long me = (Long) req.getSession().getAttribute("employeeId");
        String role = (String) req.getSession().getAttribute("role");
        JsonObject body = Json.readBody(req);
        if (body == null) { Json.error(resp, 400, "Invalid JSON body"); return; }
        String action = Json.str(body, "action");
        action = action == null ? "APPLY" : action.toUpperCase();

        try {
            switch (action) {
                case "APPLY": {
                    Long typeId = parseLong(Json.str(body, "leaveTypeId"));
                    LocalDate start = parseDate(Json.str(body, "startDate"));
                    LocalDate end = parseDate(Json.str(body, "endDate"));
                    if (typeId == null || start == null || end == null) { Json.error(resp, 400, "Leave type and valid start/end dates (yyyy-MM-dd) are required."); return; }
                    boolean half = "true".equalsIgnoreCase(Json.str(body, "halfDay"));
                    long id = service.apply(me, typeId, start, end, half, Json.str(body, "reason"));
                    Json.message(resp, 201, "Leave application #" + id + " submitted.");
                    break;
                }
                case "APPROVE":
                case "REJECT": {
                    Long id = parseLong(Json.str(body, "leaveApplicationId"));
                    if (id == null) { Json.error(resp, 400, "leaveApplicationId is required"); return; }
                    boolean approve = "APPROVE".equals(action);
                    service.decide(id, me, role, approve, Json.str(body, "comments"));
                    Json.message(resp, 200, approve ? "Leave approved successfully." : "Leave rejected successfully.");
                    break;
                }
                case "CANCEL": {
                    Long id = parseLong(Json.str(body, "leaveApplicationId"));
                    if (id == null) { Json.error(resp, 400, "leaveApplicationId is required"); return; }
                    service.cancel(id, me, Json.str(body, "comments"));
                    Json.message(resp, 200, "Leave request cancelled.");
                    break;
                }
                case "BATCH_APPROVE": {
                    if (!isStaff(role)) { Json.error(resp, 403, "Only managers or HR can batch approve leave."); return; }
                    boolean all = "HR_ADMIN".equals(role);
                    try {
                        com.google.gson.JsonArray pendingArr = applications.findPending(me, all);
                        java.util.List<JsonObject> list = new java.util.ArrayList<>();
                        for (int i = 0; i < pendingArr.size(); i++) {
                            list.add(pendingArr.get(i).getAsJsonObject());
                        }
                        LeaveApprovalQueue queue = new LeaveApprovalQueue(list);
                        java.util.List<JsonObject> batchResults = queue.processBatch(me, role, "APPROVE", Json.str(body, "comments"), service);
                        com.google.gson.JsonArray resArr = new com.google.gson.JsonArray();
                        for (JsonObject r : batchResults) resArr.add(r);
                        Json.writeArray(resp, 200, resArr);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        Json.error(resp, 500, "Database error processing batch approvals.");
                    }
                    break;
                }
                default:
                    Json.error(resp, 400, "Unknown action.");
            }
        } catch (LeaveException e) {
            Json.error(resp, e.getStatus(), e.getMessage());
        }
    }

    private static Long parseLong(String s) {
        try { return s == null ? null : Long.valueOf(s); } catch (NumberFormatException e) { return null; }
    }

    private static LocalDate parseDate(String s) {
        try { return s == null ? null : LocalDate.parse(s); } catch (DateTimeParseException e) { return null; }
    }
}
