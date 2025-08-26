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
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
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

        User user = (User) req.getSession().getAttribute("user");
        repository.addLeaveRequest(user.getUsername(), leaveType, startDate, endDate, reason);

        req.getSession().setAttribute("successMessage", "休暇申請を送信しました。");
    }

    private void handleApprove(HttpServletRequest req, HttpSession session, User user) {
        if (!"admin".equals(user.getRole())) {
            session.setAttribute("errorMessage", "承認権限がありません。");
            return;
        }
        try {
            int requestId = Integer.parseInt(req.getParameter("requestId"));
            repository.approveRequest(requestId, user.getUsername());
            session.setAttribute("successMessage", "申請を承認しました。");
        } catch (Exception e) {
            session.setAttribute("errorMessage", e + "承認の処理中にエラーが発生しました。");
        }
    }

    private void handleReject(HttpServletRequest req, HttpSession session, User user) {
        if (!"admin".equals(user.getRole())) {
            session.setAttribute("errorMessage", "却下権限がありません。");
            return;
        }
        try {
            int requestId = Integer.parseInt(req.getParameter("requestId"));
            String rejectionReason = req.getParameter("rejectionReason");
            if (rejectionReason == null || rejectionReason.isBlank()) {
                rejectionReason = req.getParameter("promptReason");
            }
            repository.rejectRequest(requestId, user.getUsername(), rejectionReason);
            session.setAttribute("successMessage", "申請を却下しました。");
        } catch (Exception e) {
            session.setAttribute("errorMessage", "却下の処理中にエラーが発生しました。");
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
