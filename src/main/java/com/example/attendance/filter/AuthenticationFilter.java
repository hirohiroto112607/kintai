
package com.example.attendance.filter;

import java.io.IOException;

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
        
        // セッションチェック
        HttpSession session = httpRequest.getSession(false);
        boolean isLoggedIn = (session != null && session.getAttribute("user") != null);
        
        System.out.println("Session exists: " + (session != null));
        System.out.println("Is logged in: " + isLoggedIn);
        
        if (!isLoggedIn) {
            // AJAXリクエストかどうかチェック
            String requestedWith = httpRequest.getHeader("X-Requested-With");
            String accept = httpRequest.getHeader("Accept");
            
            boolean isAjaxRequest = "XMLHttpRequest".equals(requestedWith) || 
                                  (accept != null && accept.contains("application/json"));
            
            System.out.println("Is AJAX request: " + isAjaxRequest);
            
            if (isAjaxRequest) {
                // AJAXリクエストの場合はJSONエラーを返す
                httpResponse.setContentType("application/json; charset=UTF-8");
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                String jsonResponse = "{\"success\":false,\"error\":\"認証が必要です\"}";
                System.out.println("Sending JSON error response: " + jsonResponse);
                httpResponse.getWriter().write(jsonResponse);
                return;
            } else {
                // 通常のリクエストの場合はログインページにリダイレクト
                System.out.println("Redirecting to login page");
                httpResponse.sendRedirect(contextPath + "/login");
                return;
            }
        }
        
        System.out.println("User authenticated, continuing...");
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // 終了処理（必要であれば実装）
    }
}
