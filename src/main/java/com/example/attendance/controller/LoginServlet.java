package com.example.attendance.controller;

import java.io.IOException;
import java.io.PrintWriter;

import com.example.attendance.dao.UserDAO;
import com.example.attendance.dto.User;
import com.example.attendance.util.TokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

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

        // AcceptヘッダーでJSONリクエストか判定
        String accept = req.getHeader("Accept");
        boolean isJsonRequest = accept != null && accept.contains("application/json");

        // 入力値の検証
        if (username == null || username.trim().isEmpty()) {
            sendErrorResponse(req, resp, isJsonRequest, "ユーザー名を入力してください。");
            return;
        }

        if (password == null || password.trim().isEmpty()) {
            sendErrorResponse(req, resp, isJsonRequest, "パスワードを入力してください。");
            return;
        }

        try {
            // パスワード検証
            if (userDAO.verifyPassword(username.trim(), password)) {
                User user = userDAO.findByUsername(username.trim());

                if (user == null) {
                    sendErrorResponse(req, resp, isJsonRequest, "ユーザーが見つかりません。");
                    return;
                }

                // アカウントが有効かチェック
                if (!user.isEnabled()) {
                    sendErrorResponse(req, resp, isJsonRequest, "アカウントが無効になっています。管理者にお問い合わせください。");
                    return;
                }

                // ログイン成功
                if (isJsonRequest) {
                    // APIリクエスト：JWTトークンを生成してJSONで返却
                    String token = TokenUtil.generateToken(user.getUsername(), user.getRole());

                    resp.setContentType("application/json; charset=UTF-8");
                    resp.setStatus(HttpServletResponse.SC_OK);

                    ObjectMapper mapper = new ObjectMapper();
                    String jsonResponse = mapper.writeValueAsString(new LoginResponse(true, "ログイン成功", token, user.getRole()));

                    PrintWriter out = resp.getWriter();
                    out.print(jsonResponse);
                    out.flush();
                } else {
                    // Webブラウザリクエスト：従来通りセッションを使用
                    HttpSession session = req.getSession();
                    session.setAttribute("user", user);

                    // 管理者と従業員でリダイレクト先を分ける
                    String redirectUrl = "/attendance";
                    resp.sendRedirect(req.getContextPath() + redirectUrl);
                }

            } else {
                sendErrorResponse(req, resp, isJsonRequest, "ユーザー名またはパスワードが正しくありません。");
            }

        } catch (Exception e) {
            // データベースエラーやその他の予期せぬエラーの処理
            sendErrorResponse(req, resp, isJsonRequest, "システムエラーが発生しました。しばらく経ってから再度お試しください。");
        }
    }

    /**
     * エラーレスポンスを送信
     */
    private void sendErrorResponse(HttpServletRequest req, HttpServletResponse resp, boolean isJsonRequest, String message) throws IOException, ServletException {
        if (isJsonRequest) {
            resp.setContentType("application/json; charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            ObjectMapper mapper = new ObjectMapper();
            String jsonResponse = mapper.writeValueAsString(new LoginResponse(false, message, null, null));

            PrintWriter out = resp.getWriter();
            out.print(jsonResponse);
            out.flush();
        } else {
            // Webブラウザの場合：JSPにエラーメッセージを設定してフォワード
            req.setAttribute("errorMessage", message);
            req.getRequestDispatcher("/login.jsp").forward(req, resp);
        }
    }

    /**
     * ログインAPIレスポンス用DTO
     */
    public static class LoginResponse {
        public boolean success;
        public String message;
        public String token;
        public String role;

        public LoginResponse(boolean success, String message, String token, String role) {
            this.success = success;
            this.message = message;
            this.token = token;
            this.role = role;
        }
    }
}
