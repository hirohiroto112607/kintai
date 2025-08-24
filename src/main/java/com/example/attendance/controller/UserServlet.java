
package com.example.attendance.controller;

import java.io.IOException;

import com.example.attendance.dao.DepartmentDAO;
import com.example.attendance.dao.UserDAO;
import com.example.attendance.dto.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/users")
public class UserServlet extends HttpServlet {
    private final UserDAO userDAO = new UserDAO();
    private final DepartmentDAO departmentDAO = new DepartmentDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 管理者のみアクセス可能
        HttpSession session = req.getSession(false);
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null || !"admin".equals(currentUser.getRole())) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }

        String action = req.getParameter("action");
        if ("edit".equals(action)) {
            String usernameToEdit = req.getParameter("username");
            User userToEdit = userDAO.findByUsername(usernameToEdit);
            req.setAttribute("userToEdit", userToEdit);
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

        req.setAttribute("users", userDAO.getAllUsers());
        req.setAttribute("departments", departmentDAO.getEnabledDepartments());
        req.getRequestDispatcher("/jsp/user_management.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession(false);
        User currentUser = (User) session.getAttribute("user");

        if (currentUser == null || !"admin".equals(currentUser.getRole())) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }

        String action = req.getParameter("action");

        switch (action) {
            case "add":
                handleAddUser(req, session);
                break;
            case "update":
                handleUpdateUser(req, session);
                break;
            case "delete":
                String usernameToDelete = req.getParameter("username");
                userDAO.deleteUser(usernameToDelete);
                session.setAttribute("successMessage", "ユーザーを削除しました。");
                break;
            case "reset_password":
                String usernameToReset = req.getParameter("username");
                String newPassword = "password"; // デフォルトパスワード
                userDAO.resetPassword(usernameToReset, newPassword);
                session.setAttribute("successMessage", usernameToReset + "のパスワードをリセットしました。(新しいパスワード: " + newPassword + ")");
                break;
            case "toggle_enabled":
                String usernameToToggle = req.getParameter("username");
                boolean currentStatus = userDAO.findByUsername(usernameToToggle).isEnabled();
                userDAO.toggleUserEnabled(usernameToToggle, !currentStatus);
                session.setAttribute("successMessage", usernameToToggle + "のアカウントを" + (!currentStatus ? "有効" : "無効") + "にしました。");
                break;
        }
        resp.sendRedirect(req.getContextPath() + "/users");
    }

    private void handleAddUser(HttpServletRequest req, HttpSession session) {
        String username = req.getParameter("username");
        if (userDAO.findByUsername(username) != null) {
            session.setAttribute("errorMessage", "ユーザーID '" + username + "' は既に使用されています。");
            return;
        }
        String password = req.getParameter("password");
        String role = req.getParameter("role");
        String departmentId = req.getParameter("departmentId");
        if (departmentId != null && departmentId.trim().isEmpty()) {
            departmentId = null;
        }
        User newUser = new User(username, UserDAO.hashPassword(password), role, departmentId, true);
        userDAO.addUser(newUser);
        session.setAttribute("successMessage", "ユーザー '" + username + "' を追加しました。");
    }

    private void handleUpdateUser(HttpServletRequest req, HttpSession session) {
        String username = req.getParameter("username");
        User existingUser = userDAO.findByUsername(username);
        if (existingUser != null) {
            String role = req.getParameter("role");
            String departmentId = req.getParameter("departmentId");
            if (departmentId != null && departmentId.trim().isEmpty()) {
                departmentId = null;
            }
            boolean enabled = "true".equals(req.getParameter("enabled"));
            String password = req.getParameter("password");
            
            existingUser.setRole(role);
            existingUser.setDepartmentId(departmentId);
            existingUser.setEnabled(enabled);
            
            // パスワードが入力されている場合のみ更新
            if (password != null && !password.trim().isEmpty()) {
                existingUser.setPassword(UserDAO.hashPassword(password));
            }
            
            userDAO.updateUser(existingUser);
            session.setAttribute("successMessage", "ユーザー '" + username + "' の情報を更新しました。");
        }
    }
}
