
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
 * 認証フィルタ。
 * ログインしていないユーザーが保護されたページにアクセスするのを防ぎます。
 */
@WebFilter("/*") // すべてのリクエストに適用
public class AuthenticationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String uri = req.getRequestURI();

        // ログインページ、CSS、ログイン処理自体はフィルタ対象外
        if (uri.endsWith("login.jsp") || uri.endsWith("login") || uri.endsWith(".css")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false); // 既存のセッションを取得

        boolean loggedIn = (session != null && session.getAttribute("user") != null);

        if (loggedIn) {
            // ログイン済みの場合、リクエストを続行
            chain.doFilter(request, response);
        } else {
            // 未ログインの場合、ログインページにリダイレクト
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 初期化処理 (今回は不要)
    }

    @Override
    public void destroy() {
        // 破棄処理 (今回は不要)
    }
}
