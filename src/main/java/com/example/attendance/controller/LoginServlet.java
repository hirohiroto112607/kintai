
package com.example.attendance.controller;

import java.io.IOException;

import com.example.attendance.dao.UserDAO;
import com.example.attendance.dto.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (userDAO.verifyPassword(username, password)) {
            User user = userDAO.findByUsername(username);
            HttpSession session = req.getSession(); // 新しいセッションを作成
            session.setAttribute("user", user);
            
            if ("admin".equals(user.getRole())) {
                resp.sendRedirect(req.getContextPath() + "/attendance");
            } else {
                resp.sendRedirect(req.getContextPath() + "/attendance");
            }
        } else {
            req.setAttribute("errorMessage", "ユーザーIDまたはパスワードが不正です。またはアカウントが無効です。");
            req.getRequestDispatcher("/login.jsp").forward(req, resp);
        }
    }
}
