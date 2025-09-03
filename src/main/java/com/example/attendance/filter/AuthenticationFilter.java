
package com.example.attendance.filter;

import java.io.IOException;
import java.io.PrintWriter;

import com.example.attendance.dao.UserDAO;
import com.example.attendance.dto.User;
import com.example.attendance.util.TokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 認証フィルタ - セッション管理とAJAXリクエストの認証を処理
 */
@WebFilter("/*")
public class AuthenticationFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 初期化処理（必要であれば実装）
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // 認証が不要なパス
        String requestURI = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        String path = requestURI.substring(contextPath.length());
        
        // デバッグ情報をログに出力
        System.out.println("=== AuthenticationFilter ===");
        System.out.println("Request URI: " + requestURI);
        System.out.println("Context Path: " + contextPath);
        System.out.println("Path: " + path);
        System.out.println("Method: " + httpRequest.getMethod());
        System.out.println("Accept Header: " + httpRequest.getHeader("Accept"));
        System.out.println("X-Requested-With: " + httpRequest.getHeader("X-Requested-With"));
        
        // 認証が不要なパスをチェック
        if (path.equals("/login") || path.equals("/login.jsp") || 
            path.startsWith("/style.css") || path.startsWith("/static/") ||
            path.startsWith("/passkey/")) {
            System.out.println("Skipping authentication for: " + path);
            chain.doFilter(request, response);
            return;
        }
        
        // APIリクエストかWebブラウザリクエストかを判定
        String accept = httpRequest.getHeader("Accept");
        String authorization = httpRequest.getHeader("Authorization");
        boolean isApiRequest = (accept != null && accept.contains("application/json")) ||
                              (authorization != null && authorization.startsWith("Bearer "));

        System.out.println("Authorization header: " + (authorization != null ? "present" : "null"));
        System.out.println("Is API request: " + isApiRequest);

        boolean isAuthenticated = false;
        User authenticatedUser = null;

        if (isApiRequest) {
            // APIリクエスト：JWTトークン検証
            if (authorization != null && authorization.startsWith("Bearer ")) {
                String token = authorization.substring(7); // "Bearer "の後ろを取得
                try {
                    String username = TokenUtil.getUsernameFromToken(token);
                    String role = TokenUtil.getRoleFromToken(token);

                    // トークンから取得したユーザー情報を検証
                    UserDAO userDAO = new UserDAO();
                    User user = userDAO.findByUsername(username);

                    if (user != null && user.isEnabled() && role.equals(user.getRole())) {
                        isAuthenticated = true;
                        authenticatedUser = user;
                        System.out.println("Token authentication successful for user: " + username);
                    } else {
                        System.out.println("Token authentication failed: user not found or disabled");
                    }
                } catch (Exception e) {
                    System.out.println("Token validation failed: " + e.getMessage());
                }
            }
        } else {
            // Webブラウザリクエスト：セッションチェック
            HttpSession session = httpRequest.getSession(false);
            isAuthenticated = (session != null && session.getAttribute("user") != null);
            if (isAuthenticated) {
                authenticatedUser = (User) session.getAttribute("user");
            }
            System.out.println("Session exists: " + (session != null));
            System.out.println("Is logged in: " + isAuthenticated);
        }

        if (!isAuthenticated) {
            if (isApiRequest) {
                // APIリクエストの場合はJSONエラーを返す
                httpResponse.setContentType("application/json; charset=UTF-8");
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                String jsonResponse = "{\"success\":false,\"error\":\"認証が必要です\"}";
                System.out.println("Sending JSON error response: " + jsonResponse);
                PrintWriter writer = httpResponse.getWriter();
                writer.write(jsonResponse);
                writer.flush();
                return;
            } else {
                // Webブラウザリクエストの場合はログインページにリダイレクト
                System.out.println("Redirecting to login page");
                httpResponse.sendRedirect(contextPath + "/login");
                return;
            }
        }

        // 認証済みユーザーをリクエスト属性に設定（サーブレットで使用可能）
        if (authenticatedUser != null) {
            httpRequest.setAttribute("authenticatedUser", authenticatedUser);
        }
        
        System.out.println("User authenticated, continuing...");
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // 終了処理（必要であれば実装）
    }
}
