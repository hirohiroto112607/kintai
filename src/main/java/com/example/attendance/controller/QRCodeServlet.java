package com.example.attendance.controller;

import java.io.IOException;

import com.example.attendance.dao.AttendanceDAO;
import com.example.attendance.dao.UserDAO;
import com.example.attendance.dto.User;
import com.example.attendance.util.QRCodeUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * QRコードによる打刻機能を提供するサーブレット
 */
@WebServlet("/qr")
public class QRCodeServlet extends HttpServlet {
    
    private final AttendanceDAO attendanceDAO = new AttendanceDAO();
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String action = req.getParameter("action");
        String view = req.getParameter("view");
        
        HttpSession session = req.getSession(false);
        if (session == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }
        
        // スキャナー画面の表示
        if ("scanner".equals(view)) {
            req.getRequestDispatcher("/jsp/qr_scanner.jsp").forward(req, resp);
            return;
        }
        
        // ユーザーIDQRコード生成の場合
        if ("user_id".equals(action)) {
            generateUserIdQRCode(req, resp);
            return;
        }

        // ユーザーIDベースの打刻処理
        String scannedUserId = req.getParameter("userId");
        if (scannedUserId != null) {
            processUserIdScan(req, resp, scannedUserId);
            return;
        }        // QRコード画面の表示
        req.getRequestDispatcher("/jsp/qr_menu.jsp").forward(req, resp);
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String action = req.getParameter("action");
        System.out.println("QRCodeServlet POST - action: " + action); // デバッグログ
        
        if ("user_id_scan".equals(action)) {
            String scannedUserId = req.getParameter("userId");
            System.out.println("QRCodeServlet POST - scannedUserId: " + scannedUserId); // デバッグログ
            
            if (scannedUserId != null && !scannedUserId.trim().isEmpty()) {
                processUserIdScanAjax(req, resp, scannedUserId.trim());
            } else {
                resp.setContentType("application/json; charset=UTF-8");
                resp.setCharacterEncoding("UTF-8");
                String jsonResponse = "{\"success\": false, \"error\": \"有効なユーザーIDを入力してください。\"}";
                resp.getWriter().write(jsonResponse);
                resp.getWriter().flush();
                System.out.println("QRCodeServlet POST - Empty userId error response sent"); // デバッグログ
            }
        } else {
            // 不明なアクション
            resp.setContentType("application/json; charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");
            String jsonResponse = "{\"success\": false, \"error\": \"不明なアクションです。\"}";
            resp.getWriter().write(jsonResponse);
            resp.getWriter().flush();
            System.out.println("QRCodeServlet POST - Unknown action error response sent: " + action); // デバッグログ
        }
    }
    


    /**
     * ユーザーIDのQRコードを生成してレスポンスとして返す
     */
    private void generateUserIdQRCode(HttpServletRequest req, HttpServletResponse resp) 
            throws IOException {
        
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");
        
        if (user == null) {
            resp.setContentType("application/json; charset=UTF-8");
            resp.getWriter().write("{\"success\": false, \"error\": \"ユーザー情報が見つかりません。\"}");
            return;
        }
        
        try {
            // ユーザーIDをQRコードとして生成
            String qrData = QRCodeUtil.generateUserIdQRCode(user.getUsername());
            
            // QRコード画像を生成
            String qrCodeImage = QRCodeUtil.generateQRCodeImage(qrData, 300, 300);
            
            // JSONレスポンスを返す
            resp.setContentType("application/json; charset=UTF-8");
            resp.getWriter().write(String.format(
                "{\"success\": true, \"qrCodeImage\": \"data:image/png;base64,%s\", \"userId\": \"%s\", \"type\": \"user_id\"}",
                qrCodeImage, user.getUsername()
            ));
            
        } catch (Exception e) {
            resp.setContentType("application/json; charset=UTF-8");
            resp.getWriter().write("{\"success\": false, \"error\": \"QRコードの生成に失敗しました。\"}");
        }
    }

    /**
     * ユーザーIDスキャンによる打刻を処理する
     */
    private void processUserIdScan(HttpServletRequest req, HttpServletResponse resp, String scannedUserId) 
            throws IOException, ServletException {
        
        HttpSession session = req.getSession();
        
        try {
            // セッションからユーザー情報を取得
            User sessionUser = (User) session.getAttribute("user");
            if (sessionUser == null) {
                req.setAttribute("errorMessage", "セッションが無効です。再度ログインしてください。");
                req.getRequestDispatcher("/jsp/qr_scanner.jsp").forward(req, resp);
                return;
            }
            
            // スキャンされたユーザーIDと現在のユーザーIDが一致するかチェック
            // if (!sessionUser.getUsername().equals(scannedUserId)) {
            //     req.setAttribute("errorMessage", "他のユーザーのQRコードです。自分のQRコードを使用してください。");
            //     req.getRequestDispatcher("/jsp/qr_scanner.jsp").forward(req, resp);
            //     return;
            // }
            
            // 現在の出勤状況をチェック
            boolean isCurrentlyCheckedIn = attendanceDAO.isCurrentlyCheckedIn(sessionUser.getUsername());
            
            if (isCurrentlyCheckedIn) {
                // 出勤中の場合は退勤処理
                boolean success = attendanceDAO.checkOut(sessionUser.getUsername());
                if (success) {
                    req.setAttribute("successMessage", "退勤が記録されました。お疲れさまでした！");
                } else {
                    req.setAttribute("errorMessage", "退勤の記録に失敗しました。");
                }
            } else {
                // 未出勤の場合は出勤処理
                boolean success = attendanceDAO.checkIn(sessionUser.getUsername());
                if (success) {
                    req.setAttribute("successMessage", "出勤が記録されました。今日も頑張りましょう！");
                } else {
                    req.setAttribute("errorMessage", "出勤の記録に失敗しました。");
                }
            }
            
        } catch (Exception e) {
            req.setAttribute("errorMessage", "処理中にエラーが発生しました: " + e.getMessage());
        }
        
        // 結果をスキャナー画面に表示
        req.getRequestDispatcher("/jsp/qr_scanner.jsp").forward(req, resp);
    }

    /**
     * ユーザーIDスキャンによる打刻をAJAXで処理する
     */
    private void processUserIdScanAjax(HttpServletRequest req, HttpServletResponse resp, String scannedUserId) 
            throws IOException {
        
        System.out.println("processUserIdScanAjax called with userId: " + scannedUserId); // デバッグログ
        
        HttpSession session = req.getSession();
        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        
        try {
            // セッションからユーザー情報を取得
            User sessionUser = (User) session.getAttribute("user");
            if (sessionUser == null) {
                String jsonResponse = "{\"success\": false, \"error\": \"セッションが無効です。再度ログインしてください。\"}";
                System.out.println("Session user is null, sending response: " + jsonResponse); // デバッグログ
                resp.getWriter().write(jsonResponse);
                resp.getWriter().flush();
                return;
            }
            
            System.out.println("Session user: " + sessionUser.getUsername() + ", role: " + sessionUser.getRole()); // デバッグログ

            String targetUserId;
            UserDAO userDAO = new UserDAO();

            // 管理者かどうかで処理を分岐
            if ("admin".equals(sessionUser.getRole())) {
                // 管理者の場合、スキャンされたIDを処理対象とする
                targetUserId = scannedUserId;
                System.out.println("Admin user " + sessionUser.getUsername() + " is processing for user: " + targetUserId);
            } else {
                // 一般ユーザーの場合、自分のQRコードかチェック
                if (!sessionUser.getUsername().equals(scannedUserId)) {
                    String jsonResponse = "{\"success\": false, \"error\": \"他のユーザーのQRコードです。自分のQRコードを使用してください。\"}";
                    resp.getWriter().write(jsonResponse);
                    return;
                }
                targetUserId = sessionUser.getUsername();
            }

            // 処理対象のユーザーが存在するかチェック
            User targetUser = userDAO.findByUsername(targetUserId);
            if (targetUser == null) {
                String jsonResponse = "{\"success\": false, \"error\": \"スキャンされたユーザーIDは存在しません。\"}";
                resp.getWriter().write(jsonResponse);
                return;
            }
            
            // 現在の出勤状況をチェック
            boolean isCurrentlyCheckedIn = attendanceDAO.isCurrentlyCheckedIn(targetUserId);
            System.out.println("Currently checked in for " + targetUserId + ": " + isCurrentlyCheckedIn); // デバッグログ
            
            String jsonResponse;
            if (isCurrentlyCheckedIn) {
                // 出勤中の場合は退勤処理
                boolean success = attendanceDAO.checkOut(targetUserId);
                System.out.println("Check out success for " + targetUserId + ": " + success); // デバッグログ
                if (success) {
                    String message = "admin".equals(sessionUser.getRole()) && !sessionUser.getUsername().equals(targetUserId)
                                     ? String.format("%sさんの退勤が記録されました。お疲れさまでした！", targetUser.getUsername())
                                     : "退勤が記録されました。お疲れさまでした！";
                    jsonResponse = String.format("{\"success\": true, \"message\": \"%s\", \"action\": \"checkout\"}", message);
                } else {
                    jsonResponse = "{\"success\": false, \"error\": \"退勤の記録に失敗しました。\"}";
                }
            } else {
                // 未出勤の場合は出勤処理
                boolean success = attendanceDAO.checkIn(targetUserId);
                System.out.println("Check in success for " + targetUserId + ": " + success); // デバッグログ
                if (success) {
                    String message = "admin".equals(sessionUser.getRole()) && !sessionUser.getUsername().equals(targetUserId)
                                     ? String.format("%sさんの出勤が記録されました。今日も頑張りましょう！", targetUser.getUsername())
                                     : "出勤が記録されました。今日も頑張りましょう！";
                    jsonResponse = String.format("{\"success\": true, \"message\": \"%s\", \"action\": \"checkin\"}", message);
                } else {
                    jsonResponse = "{\"success\": false, \"error\": \"出勤の記録に失敗しました。\"}";
                }
            }
            
            System.out.println("Sending JSON response: " + jsonResponse); // デバッグログ
            resp.getWriter().write(jsonResponse);
            resp.getWriter().flush();
            
        } catch (Exception e) {
            String errorMessage = e.getMessage() != null ? e.getMessage().replace("\"", "\\\"") : "不明なエラー";
            String jsonResponse = "{\"success\": false, \"error\": \"処理中にエラーが発生しました: " + errorMessage + "}";
            System.out.println("Exception occurred: " + e.getMessage()); // デバッグログ
            System.out.println("Sending error response: " + jsonResponse); // デバッグログ
            e.printStackTrace(); // スタックトレースも出力
            resp.getWriter().write(jsonResponse);
            resp.getWriter().flush();
        }
    }
}
