package com.example.attendance.controller;

import java.io.IOException;
import java.util.logging.Logger;

import com.example.attendance.dao.AttendanceDAO;
import com.example.attendance.dao.UserDAO;
import com.example.attendance.dto.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 顔認証とQRコードによる勤怠打刻を処理するサーブレット
 */
@WebServlet("/face/attendance")
public class FaceAttendanceServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger(FaceAttendanceServlet.class.getName());
    private final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        User user = (User) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        // 顔データを取得して渡す（顔認証用）
        try {
            com.example.attendance.dao.FaceDataDAO faceDataDAO = new com.example.attendance.dao.FaceDataDAO();
            String faceDescriptorsJson = faceDataDAO.getAllFaceDescriptors();
            if (faceDescriptorsJson == null) {
                faceDescriptorsJson = "[]";
            }
            request.setAttribute("faceDescriptorsJson", faceDescriptorsJson);
        } catch (Exception e) {
            logger.warning("Failed to load face descriptors: " + e.getMessage());
            request.setAttribute("faceDescriptorsJson", "[]");
        }

        request.getRequestDispatcher("/jsp/face_qr_attendance.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        logger.info("FaceAttendanceServlet POST - action: " + action);

        if ("face_checkin".equals(action)) {
            handleFaceAttendance(request, response);
        } else if ("qr_checkin".equals(action)) {
            handleQRAttendance(request, response);
        } else {
            response.setContentType("application/json; charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            String jsonResponse = "{\"success\": false, \"error\": \"不明なアクションです。\"}";
            response.getWriter().write(jsonResponse);
            response.getWriter().flush();
        }
    }

    /**
     * 顔認証による打刻処理
     */
    private void handleFaceAttendance(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession();
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        try {
            // セッションからユーザー情報を取得
            User sessionUser = (User) session.getAttribute("user");
            if (sessionUser == null) {
                String jsonResponse = "{\"success\": false, \"error\": \"セッションが無効です。再度ログインしてください。\"}";
                response.getWriter().write(jsonResponse);
                return;
            }

            String detectedUsername = request.getParameter("username");
            if (detectedUsername == null || detectedUsername.trim().isEmpty()) {
                String jsonResponse = "{\"success\": false, \"error\": \"ユーザー名が指定されていません。\"}";
                response.getWriter().write(jsonResponse);
                return;
            }

            // 検知されたユーザーが存在するかチェック
            User detectedUser = userDAO.findByUsername(detectedUsername.trim());
            if (detectedUser == null) {
                logger.warning("顔認証: 検知されたユーザーが存在しません - username: " + detectedUsername.trim());
                String jsonResponse = "{\"success\": false, \"error\": \"検知されたユーザーが存在しません。\"}";
                response.getWriter().write(jsonResponse);
                return;
            }

            logger.info("顔認証: ユーザー検知成功 - username: " + detectedUsername.trim() + ", sessionUser: " + sessionUser.getUsername());

            // 現在の出勤状況をチェック
            boolean isCurrentlyCheckedIn = attendanceDAO.isCurrentlyCheckedIn(detectedUsername);

            String jsonResponse;
            if (isCurrentlyCheckedIn) {
                // 出勤中の場合は退勤処理
                boolean success = attendanceDAO.checkOut(detectedUsername);
                if (success) {
                    String message = sessionUser.getRole().equals("admin") && !sessionUser.getUsername().equals(detectedUsername)
                                     ? String.format("%sさんの退勤が記録されました。お疲れさまでした！", detectedUsername)
                                     : "退勤が記録されました。お疲れさまでした！";
                    jsonResponse = String.format("{\"success\": true, \"message\": \"%s\", \"action\": \"checkout\", \"user\": \"%s\"}", message, detectedUser.getUsername());
                } else {
                    jsonResponse = "{\"success\": false, \"error\": \"退勤の記録に失敗しました。\"}";
                }
            } else {
                // 未出勤の場合は出勤処理
                boolean success = attendanceDAO.checkIn(detectedUsername);
                if (success) {
                    String message = sessionUser.getRole().equals("admin") && !sessionUser.getUsername().equals(detectedUsername)
                                     ? String.format("%sさんの出勤が記録されました。今日も頑張りましょう！", detectedUser.getUsername())
                                     : "出勤が記録されました。今日も頑張りましょう！";
                    jsonResponse = String.format("{\"success\": true, \"message\": \"%s\", \"action\": \"checkin\", \"user\": \"%s\"}", message, detectedUser.getUsername());
                } else {
                    jsonResponse = "{\"success\": false, \"error\": \"出勤の記録に失敗しました。\"}";
                }
            }

            logger.info("Face attendance result: " + jsonResponse);
            response.getWriter().write(jsonResponse);
            response.getWriter().flush();

        } catch (Exception e) {
            logger.severe("Exception in handleFaceAttendance: " + e.getMessage());
            String errorMessage = e.getMessage() != null ? e.getMessage().replace("\"", "\\\"") : "不明なエラー";
            String jsonResponse = "{\"success\": false, \"error\": \"処理中にエラーが発生しました: " + errorMessage + "}";
            response.getWriter().write(jsonResponse);
            response.getWriter().flush();
        }
    }

    /**
     * QRコードによる打刻処理
     */
    private void handleQRAttendance(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession();
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        logger.info("handleQRAttendance: 開始");

        try {
            // セッションからユーザー情報を取得
            User sessionUser = (User) session.getAttribute("user");
            if (sessionUser == null) {
                logger.warning("handleQRAttendance: セッションが無効");
                String jsonResponse = "{\"success\": false, \"error\": \"セッションが無効です。再度ログインしてください。\"}";
                response.getWriter().write(jsonResponse);
                return;
            }

            logger.info("handleQRAttendance: セッションユーザー - " + sessionUser.getUsername() + " (role: " + sessionUser.getRole() + ")");

            String scannedUserId = request.getParameter("userId");
            logger.info("handleQRAttendance: scannedUserId = '" + scannedUserId + "'");

            if (scannedUserId == null || scannedUserId.trim().isEmpty()) {
                logger.warning("handleQRAttendance: ユーザーIDが指定されていない");
                String jsonResponse = "{\"success\": false, \"error\": \"ユーザーIDが指定されていません。\"}";
                response.getWriter().write(jsonResponse);
                return;
            }

            // 処理対象のユーザーを決定（管理者ならスキャンされたID、一般ユーザーなら自分のみ）
            String targetUserId;
            if ("admin".equals(sessionUser.getRole())) {
                targetUserId = scannedUserId.trim();
            } else {
                if (!sessionUser.getUsername().equals(scannedUserId.trim())) {
                    String jsonResponse = "{\"success\": false, \"error\": \"他のユーザーのQRコードです。自分のQRコードを使用してください。\"}";
                    response.getWriter().write(jsonResponse);
                    return;
                }
                targetUserId = sessionUser.getUsername();
            }

            // 処理対象のユーザーが存在するかチェック
            User targetUser = userDAO.findByUsername(targetUserId);
            if (targetUser == null) {
                String jsonResponse = "{\"success\": false, \"error\": \"スキャンされたユーザーIDは存在しません。\"}";
                response.getWriter().write(jsonResponse);
                return;
            }

            // 現在の出勤状況をチェック
            boolean isCurrentlyCheckedIn = attendanceDAO.isCurrentlyCheckedIn(targetUserId);

            String jsonResponse;
            if (isCurrentlyCheckedIn) {
                // 出勤中の場合は退勤処理
                boolean success = attendanceDAO.checkOut(targetUserId);
                if (success) {
                    String message = "admin".equals(sessionUser.getRole()) && !sessionUser.getUsername().equals(targetUserId)
                                     ? String.format("%sさんの退勤が記録されました。お疲れさまでした！", targetUser.getUsername())
                                     : "退勤が記録されました。お疲れさまでした！";
                    jsonResponse = String.format("{\"success\": true, \"message\": \"%s\", \"action\": \"checkout\", \"user\": \"%s\"}", message, targetUser.getUsername());
                } else {
                    jsonResponse = "{\"success\": false, \"error\": \"退勤の記録に失敗しました。\"}";
                }
            } else {
                // 未出勤の場合は出勤処理
                boolean success = attendanceDAO.checkIn(targetUserId);
                if (success) {
                    String message = "admin".equals(sessionUser.getRole()) && !sessionUser.getUsername().equals(targetUserId)
                                     ? String.format("%sさんの出勤が記録されました。今日も頑張りましょう！", targetUser.getUsername())
                                     : "出勤が記録されました。今日も頑張りましょう！";
                    jsonResponse = String.format("{\"success\": true, \"message\": \"%s\", \"action\": \"checkin\", \"user\": \"%s\"}", message, targetUser.getUsername());
                } else {
                    jsonResponse = "{\"success\": false, \"error\": \"出勤の記録に失敗しました。\"}";
                }
            }

            logger.info("QR attendance result: " + jsonResponse);
            response.getWriter().write(jsonResponse);
            response.getWriter().flush();

        } catch (Exception e) {
            logger.severe("Exception in handleQRAttendance: " + e.getMessage());
            String errorMessage = e.getMessage() != null ? e.getMessage().replace("\"", "\\\"") : "不明なエラー";
            String jsonResponse = "{\"success\": false, \"error\": \"処理中にエラーが発生しました: " + errorMessage + "}";
            response.getWriter().write(jsonResponse);
            response.getWriter().flush();
        }
    }
}
