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

        // 入力値の検証
        if (username == null || username.trim().isEmpty()) {
            req.setAttribute("errorMessage", "ユーザー名を入力してください。");
            req.getRequestDispatcher("/login.jsp").forward(req, resp);
            return;
        }

        if (password == null || password.trim().isEmpty()) {
            req.setAttribute("errorMessage", "パスワードを入力してください。");
            req.getRequestDispatcher("/login.jsp").forward(req, resp);
            return;
        }

        try {
            // パスワード検証
            if (userDAO.verifyPassword(username.trim(), password)) {
                User user = userDAO.findByUsername(username.trim());
                
                if (user == null) {
                    req.setAttribute("errorMessage", "ユーザーが見つかりません。");
                    req.getRequestDispatcher("/login.jsp").forward(req, resp);
                    return;
                }

                // アカウントが有効かチェック
                if (!user.isEnabled()) {
                    req.setAttribute("errorMessage", "アカウントが無効になっています。管理者にお問い合わせください。");
                    req.getRequestDispatcher("/login.jsp").forward(req, resp);
                    return;
                }

                // ログイン成功
                HttpSession session = req.getSession();
                session.setAttribute("user", user);
                
                // 管理者と従業員でリダイレクト先を分ける
                String redirectUrl = "/attendance";
                resp.sendRedirect(req.getContextPath() + redirectUrl);
                
            } else {
                req.setAttribute("errorMessage", "ユーザー名またはパスワードが正しくありません。");
                req.getRequestDispatcher("/login.jsp").forward(req, resp);
            }
            
        } catch (Exception e) {
            // データベースエラーやその他の予期せぬエラーの処理
            req.setAttribute("errorMessage", "システムエラーが発生しました。しばらく経ってから再度お試しください。");
            req.getRequestDispatcher("/login.jsp").forward(req, resp);
        }
    }
}
