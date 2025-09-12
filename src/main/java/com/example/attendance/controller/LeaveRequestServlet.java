package com.example.attendance.controller;

import java.io.IOException;

import com.example.attendance.dao.LeaveRequestRepository;
import com.example.attendance.dto.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/leave-requests")
public class LeaveRequestServlet extends HttpServlet {
    private final LeaveRequestRepository repository = new LeaveRequestRepository();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }

        User currentUser = (User) session.getAttribute("user");

        // フラッシュメッセージを移送
        moveFlash(session, req, "successMessage");
        moveFlash(session, req, "errorMessage");

        if ("admin".equals(currentUser.getRole())) {
            req.setAttribute("pendingRequests", repository.findPendingRequests());
            req.setAttribute("allRequests", repository.findAllRequests());
            req.getRequestDispatcher("/jsp/leave_management.jsp").forward(req, resp);
        } else {
            req.setAttribute("leaveRequests", repository.findByUserId(currentUser.getUsername()));
            req.getRequestDispatcher("/jsp/leave_requests.jsp").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession(false);
        String employeeId = req.getParameter("employeeId");

        // セッションがない場合、employeeIdで認証をバイパス（モバイルアプリ用）
        if (session == null || session.getAttribute("user") == null) {
            if (employeeId == null || employeeId.trim().isEmpty()) {
                resp.sendRedirect(req.getContextPath() + "/login.jsp");
                return;
            }
            // employeeIdでセッションを作成（簡易実装）
            User dummyUser = new User();
            dummyUser.setUsername(employeeId);
            dummyUser.setRole("employee");
            session = req.getSession(true);
            session.setAttribute("user", dummyUser);
        }

        User currentUser = (User) session.getAttribute("user");
        String action = req.getParameter("action");

        if ("apply".equals(action)) {
            handleApply(req);
        } else if ("approve".equals(action)) {
            handleApprove(req, session, currentUser);
        } else if ("reject".equals(action)) {
            handleReject(req, session, currentUser);
        }

        resp.sendRedirect(req.getContextPath() + "/leave-requests");
    }

    private void handleApply(HttpServletRequest req) {
        String leaveType = req.getParameter("leaveType");
        String startDate = req.getParameter("startDate");
        String endDate = req.getParameter("endDate");
        String reason = req.getParameter("reason");
        String employeeId = req.getParameter("employeeId");

        // セッションからユーザー取得、またはemployeeIdを使用
        User user = (User) req.getSession().getAttribute("user");
        String username = user != null ? user.getUsername() : employeeId;

        if (username == null || username.trim().isEmpty()) {
            req.getSession().setAttribute("errorMessage", "ユーザーが特定できません。");
            return;
        }

        repository.addLeaveRequest(username, leaveType, startDate, endDate, reason);

        req.getSession().setAttribute("successMessage", "休暇申請を送信しました。");
    }

    private void handleApprove(HttpServletRequest req, HttpSession session, User user) {
        if (!"admin".equals(user.getRole())) {
            session.setAttribute("errorMessage", "承認権限がありません。");
            return;
        }
        try {
            String requestIdStr = req.getParameter("requestId");
            
            if (requestIdStr == null || requestIdStr.trim().isEmpty()) {
                session.setAttribute("errorMessage", "申請IDが指定されていません。");
                return;
            }
            
            int requestId = Integer.parseInt(requestIdStr);
            boolean success = repository.approveRequest(requestId, user.getUsername());
            
            if (success) {
                session.setAttribute("successMessage", "申請を承認しました。");
            } else {
                session.setAttribute("errorMessage", "申請の承認に失敗しました。");
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid request ID format: " + req.getParameter("requestId"));
            session.setAttribute("errorMessage", "申請IDの形式が正しくありません。");
        } catch (Exception e) {
            System.err.println("Error in handleApprove: " + e.getMessage());
            e.printStackTrace();
            session.setAttribute("errorMessage", "承認の処理中にエラーが発生しました：" + e.getMessage());
        }
    }

    private void handleReject(HttpServletRequest req, HttpSession session, User user) {
        if (!"admin".equals(user.getRole())) {
            session.setAttribute("errorMessage", "却下権限がありません。");
            return;
        }
        try {
            String requestIdStr = req.getParameter("requestId");
            String rejectionReason = req.getParameter("rejectionReason");
            
            // デバッグ用ログ
            System.out.println("DEBUG - handleReject called");
            System.out.println("DEBUG - requestId parameter: " + requestIdStr);
            System.out.println("DEBUG - rejectionReason parameter: " + rejectionReason);
            
            if (requestIdStr == null || requestIdStr.trim().isEmpty()) {
                session.setAttribute("errorMessage", "申請IDが指定されていません。");
                return;
            }
            
            int requestId = Integer.parseInt(requestIdStr);
            
            if (rejectionReason == null || rejectionReason.isBlank()) {
                rejectionReason = req.getParameter("promptReason");
                if (rejectionReason == null || rejectionReason.isBlank()) {
                    rejectionReason = "理由未記入";
                }
            }
            
            System.out.println("DEBUG - About to call repository.rejectRequest with ID: " + requestId + ", reason: " + rejectionReason);
            
            boolean success = repository.rejectRequest(requestId, user.getUsername(), rejectionReason);
            
            if (success) {
                session.setAttribute("successMessage", "申請を却下しました。");
            } else {
                session.setAttribute("errorMessage", "申請の却下に失敗しました。");
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid request ID format: " + req.getParameter("requestId"));
            session.setAttribute("errorMessage", "申請IDの形式が正しくありません。");
        } catch (Exception e) {
            System.err.println("Error in handleReject: " + e.getMessage());
            e.printStackTrace();
            session.setAttribute("errorMessage", "却下の処理中にエラーが発生しました：" + e.getMessage());
        }
    }

    private static void moveFlash(HttpSession session, HttpServletRequest req, String name) {
        Object v = session.getAttribute(name);
        if (v != null) {
            req.setAttribute(name, v);
            session.removeAttribute(name);
        }
    }
}
