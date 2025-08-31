package com.example.attendance.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.attendance.dao.AttendanceDAO;
import com.example.attendance.dao.UserDAO;
import com.example.attendance.dto.Attendance;
import com.example.attendance.dto.User;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(urlPatterns = {"/attendance", "/attendance/status"})
public class AttendanceServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AttendanceServlet.class);
    private final AttendanceDAO attendanceDAO = new AttendanceDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        String action = req.getParameter("action");
        
        // デバッグ情報をログに出力
        logger.debug("=== AttendanceServlet.doGet ===");
        logger.debug("Request URI: {}", req.getRequestURI());
        logger.debug("Action parameter: {}", action);
        logger.debug("Session exists: {}", session != null);
        logger.debug("User in session: {}", session != null && session.getAttribute("user") != null ? 
                          ((User) session.getAttribute("user")).getUsername() : "null");
        
        // テスト用のHTMLページを提供
        if ("test".equals(action)) {
            resp.sendRedirect(req.getContextPath() + "/jsp/test_nfc.jsp");
            return;
        }
        
        if (session == null || session.getAttribute("user") == null) {
            // AJAX リクエストの場合はJSONエラーレスポンスを返す
            String requestedWith = req.getHeader("X-Requested-With");
            String accept = req.getHeader("Accept");
            
            if ("XMLHttpRequest".equals(requestedWith) || 
                (accept != null && accept.contains("application/json"))) {
                resp.setContentType("application/json; charset=UTF-8");
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write("{\"success\":false,\"error\":\"認証が必要です\"}");
                return;
            }
            
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        User user = (User) session.getAttribute("user");
        
        // 明示的な /attendance/status エンドポイントを追加でサポート
        String servletPath = req.getServletPath();
        if ("/attendance/status".equals(servletPath) || "get_status".equals(action)) {
            getAttendanceStatus(req, resp, user);
            return;
        }

        if ("export_csv".equals(action) && "admin".equals(user.getRole())) {
            exportCsv(req, resp);
            return;
        }

        // メッセージをセッションからリクエストスコープへ移動
        String successMessage = (String) session.getAttribute("successMessage");
        if (successMessage != null) {
            req.setAttribute("successMessage", successMessage);
            session.removeAttribute("successMessage");
        }
        String errorMessage = (String) session.getAttribute("errorMessage");
        if (errorMessage != null) {
            req.setAttribute("errorMessage", errorMessage);
            session.removeAttribute("errorMessage");
        }

        if ("admin".equals(user.getRole())) {
            handleAdminView(req, resp);
        } else {
            handleEmployeeView(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");
        String action = req.getParameter("action");

        if ("nfc_attendance".equals(action)) {
            handleNFCAttendance(req, resp, user);
            return;
        }

        switch (action) {
            case "check_in":
                attendanceDAO.checkIn(user.getUsername());
                session.setAttribute("successMessage", "出勤を記録しました。");
                break;
            case "check_out":
                attendanceDAO.checkOut(user.getUsername());
                session.setAttribute("successMessage", "退勤を記録しました。");
                break;
            case "add_manual":
                if ("admin".equals(user.getRole()))
                    handleAddManual(req, session);
                break;
            case "delete_manual":
                if ("admin".equals(user.getRole()))
                    handleDeleteManual(req, session);
                break;
        }
        resp.sendRedirect(req.getContextPath() + "/attendance");
    }

    private void handleAdminView(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String filterUserId = req.getParameter("filterUserId");
        String startDateStr = req.getParameter("startDate");
        String endDateStr = req.getParameter("endDate");

        LocalDate startDate = null;
        LocalDate endDate = null;
        try {
            if (startDateStr != null && !startDateStr.isEmpty())
                startDate = LocalDate.parse(startDateStr);
            if (endDateStr != null && !endDateStr.isEmpty())
                endDate = LocalDate.parse(endDateStr);
        } catch (DateTimeParseException e) {
            req.setAttribute("errorMessage", "日付の形式が不正です。YYYY-MM-DD形式で入力してください。");
        }

        List<Attendance> records = attendanceDAO.findFilteredRecords(filterUserId, startDate, endDate);
        req.setAttribute("allAttendanceRecords", records);

        // ユーザー毎の合計労働時間
        Map<String, Long> totalHoursByUser = records.stream()
                .filter(att -> att.getCheckInTime() != null && att.getCheckOutTime() != null)
                .collect(Collectors.groupingBy(Attendance::getUserId,
                        Collectors.summingLong(att -> java.time.temporal.ChronoUnit.HOURS.between(att.getCheckInTime(),
                                att.getCheckOutTime()))));
        req.setAttribute("totalHoursByUser", totalHoursByUser);

        // 月別データ
        req.setAttribute("monthlyWorkingHours", attendanceDAO.getMonthlyWorkingHours(filterUserId));
        req.setAttribute("monthlyCheckInCounts", attendanceDAO.getMonthlyCheckInCounts(filterUserId));

        req.getRequestDispatcher("/jsp/admin_menu.jsp").forward(req, resp);
    }

    private void handleEmployeeView(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = (User) req.getSession().getAttribute("user");
        req.setAttribute("attendanceRecords", attendanceDAO.findByUserId(user.getUsername()));
        req.getRequestDispatcher("/jsp/employee_menu.jsp").forward(req, resp);
    }

    private void handleAddManual(HttpServletRequest req, HttpSession session) {
        try {
            String userId = req.getParameter("userId");
            LocalDateTime checkIn = LocalDateTime.parse(req.getParameter("checkInTime"));
            String checkOutStr = req.getParameter("checkOutTime");
            LocalDateTime checkOut = (checkOutStr != null && !checkOutStr.isEmpty()) ? LocalDateTime.parse(checkOutStr)
                    : null;
            attendanceDAO.addManualAttendance(userId, checkIn, checkOut);
            session.setAttribute("successMessage", "勤怠記録を手動で追加しました。");
        } catch (DateTimeParseException e) {
            session.setAttribute("errorMessage", "日付/時刻の形式が不正です。");
        }
    }

    private void handleDeleteManual(HttpServletRequest req, HttpSession session) {
        try {
            String userId = req.getParameter("userId");
            LocalDateTime checkIn = LocalDateTime.parse(req.getParameter("checkInTime"));
            String checkOutStr = req.getParameter("checkOutTime");
            LocalDateTime checkOut = (checkOutStr != null && !checkOutStr.isEmpty()) ? LocalDateTime.parse(checkOutStr)
                    : null;

            if (attendanceDAO.deleteManualAttendance(userId, checkIn, checkOut)) {
                session.setAttribute("successMessage", "勤怠記録を削除しました。");
            } else {
                session.setAttribute("errorMessage", "該当の勤怠記録が見つからず、削除できませんでした。");
            }
        } catch (DateTimeParseException e) {
            session.setAttribute("errorMessage", "日付/時刻の形式が不正です。");
        }
    }

    private void getAttendanceStatus(HttpServletRequest req, HttpServletResponse resp, User user) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");
        
        // デバッグ情報をログに出力
        logger.debug("=== AttendanceServlet.getAttendanceStatus ===");
        logger.debug("User: {}", user != null ? user.getUsername() : "null");
        logger.debug("Request URI: {}", req.getRequestURI());
        logger.debug("Servlet Path: {}", req.getServletPath());
        logger.debug("Request Method: {}", req.getMethod());
        logger.debug("Content Type: {}", req.getContentType());
        logger.debug("Accept Header: {}", req.getHeader("Accept"));
        logger.debug("X-Requested-With: {}", req.getHeader("X-Requested-With"));
        
        if (user == null) {
            logger.error("Error: User is null in getAttendanceStatus");
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> response = Map.of(
                "success", false,
                "error", "ユーザー情報が取得できません"
            );
            resp.getWriter().write(mapper.writeValueAsString(response));
            return;
        }
        
        try {
            // 今日の最新の勤怠記録を取得
            List<Attendance> todayRecords = attendanceDAO.findByUserIdAndDate(user.getUsername(), LocalDate.now());
            
            String status = "out"; // デフォルトは退勤状態
            String lastActivity = null;
            
            if (!todayRecords.isEmpty()) {
                // 最新の記録を取得
                Attendance latestRecord = todayRecords.get(todayRecords.size() - 1);
                
                if (latestRecord.getCheckOutTime() == null) {
                    // 退勤記録がない場合は出勤中
                    status = "in";
                    lastActivity = "出勤: " + latestRecord.getCheckInTime().format(DateTimeFormatter.ofPattern("HH:mm"));
                } else {
                    // 退勤記録がある場合は退勤状態
                    status = "out";
                    lastActivity = "退勤: " + latestRecord.getCheckOutTime().format(DateTimeFormatter.ofPattern("HH:mm"));
                }
            }
            
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> response = Map.of(
                "success", true,
                "status", status,
                "lastActivity", lastActivity != null ? lastActivity : "本日の記録なし"
            );
            
            String jsonResponse = mapper.writeValueAsString(response);
            logger.debug("JSON Response: {}", jsonResponse);
            resp.getWriter().write(jsonResponse);
            
        } catch (Exception e) {
            logger.error("Error in getAttendanceStatus: {}", e.getMessage());
            logger.error("Exception class: {}", e.getClass().getName());
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> response = Map.of(
                "success", false,
                "error", "勤務状況の取得に失敗しました"
            );
            resp.getWriter().write(mapper.writeValueAsString(response));
        }
    }

    @SuppressWarnings("unused")
    private void handleNFCAttendance(HttpServletRequest req, HttpServletResponse resp, User user) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");
        
        try {
            String cardId = req.getParameter("cardId");
            String mode = req.getParameter("mode"); // 追加: モードパラメータを受け取る
            String targetUsername = null;
            
            // cardId が渡された場合は管理者端末からの社員証打刻を想定
            if (cardId != null && !cardId.isEmpty()) {
                // 管理者権限のチェック
                if (user == null || user.getRole() == null || !"admin".equalsIgnoreCase(user.getRole())) {
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> response = Map.of(
                        "success", false,
                        "error", "管理者権限が必要です"
                    );
                    resp.getWriter().write(mapper.writeValueAsString(response));
                    return;
                }
                
                // cardId は users.username に対応している想定なので照合する
                UserDAO userDAO = new UserDAO();
                User targetUser = userDAO.findByUsername(cardId);
                if (targetUser == null) {
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> response = Map.of(
                        "success", false,
                        "error", "指定された社員が見つかりません: " + cardId
                    );
                    resp.getWriter().write(mapper.writeValueAsString(response));
                    return;
                }
                targetUsername = targetUser.getUsername();
            } else {
                // cardId がない場合は従来の端末ログインユーザーを対象にする
                if (user == null) {
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> response = Map.of(
                        "success", false,
                        "error", "ログイン情報が取得できません"
                    );
                    resp.getWriter().write(mapper.writeValueAsString(response));
                    return;
                }
                targetUsername = user.getUsername();
            }
            
            // 出勤・退勤の判定（モード指定がある場合は自動判定をスキップ）
            boolean isCheckIn;
            if (mode != null && !mode.isEmpty()) {
                // モードが指定されている場合はそれに従う
                isCheckIn = "check_in".equals(mode) || "in".equals(mode);
            } else {
                // モードが指定されていない場合は自動判定
                isCheckIn = shouldCheckIn(targetUsername);
            }
            
            if (isCheckIn) {
                attendanceDAO.checkIn(targetUsername);
            } else {
                attendanceDAO.checkOut(targetUsername);
            }
            
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> response = Map.of(
                "success", true,
                "action", isCheckIn ? "check_in" : "check_out",
                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                "targetUsername", targetUsername
            );
            
            resp.getWriter().write(mapper.writeValueAsString(response));
            
        } catch (Exception e) {
            logger.error("Error in handleNFCAttendance: {}", e.getMessage());
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> response = Map.of(
                "success", false,
                "error", "勤怠打刻に失敗しました: " + e.getMessage()
            );
            resp.getWriter().write(mapper.writeValueAsString(response));
        }
    }
    
    private boolean shouldCheckIn(String username) {
        if (username == null) {
            return true; // デフォルトは出勤
        }
        // 今日の最新の勤怠記録を取得
        List<Attendance> todayRecords = attendanceDAO.findByUserIdAndDate(username, LocalDate.now());
        
        if (todayRecords.isEmpty()) {
            // 今日の記録がない場合は出勤
            return true;
        }
        
        // 最新の記録を取得
        Attendance latestRecord = todayRecords.get(todayRecords.size() - 1);
        
        // 退勤記録がない場合は退勤、ある場合は出勤
        return latestRecord.getCheckOutTime() != null;
    }

    private void exportCsv(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/csv; charset=UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=\"attendance_records.csv\"");

        String filterUserId = req.getParameter("filterUserId");
        String startDateStr = req.getParameter("startDate");
        String endDateStr = req.getParameter("endDate");

        LocalDate startDate = null;
        LocalDate endDate = null;
        try {
            if (startDateStr != null && !startDateStr.isEmpty())
                startDate = LocalDate.parse(startDateStr);
            if (endDateStr != null && !endDateStr.isEmpty())
                endDate = LocalDate.parse(endDateStr);
        } catch (DateTimeParseException e) {
            // Ignore parse error for CSV export
        }

        List<Attendance> records = attendanceDAO.findFilteredRecords(filterUserId, startDate, endDate);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try (PrintWriter writer = resp.getWriter()) {
            writer.append("ユーザーID,出勤時刻,退勤時刻\n");
            for (Attendance record : records) {
                String checkIn = record.getCheckInTime() != null ? record.getCheckInTime().format(formatter) : "";
                String checkOut = record.getCheckOutTime() != null ? record.getCheckOutTime().format(formatter) : "";
                writer.append(String.format("%s,%s,%s\n", record.getUserId(), checkIn, checkOut));
            }
            writer.flush();
        }
    }
}
