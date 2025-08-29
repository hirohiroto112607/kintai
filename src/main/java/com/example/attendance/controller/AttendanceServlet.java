
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

import com.example.attendance.dao.AttendanceDAO;
import com.example.attendance.dto.Attendance;
import com.example.attendance.dto.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/attendance")
public class AttendanceServlet extends HttpServlet {
    private final AttendanceDAO attendanceDAO = new AttendanceDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        if (session.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        User user = (User) session.getAttribute("user");

        String action = req.getParameter("action");

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
