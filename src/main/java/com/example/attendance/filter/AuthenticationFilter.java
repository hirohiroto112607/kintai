
package com.example.attendance.filter;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;

/**
 * 最小実装の認証フィルタ（現時点ではパススルー）。
 * 将来的に認証ロジックを追加すること。
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
        // 現在はパススルー。後で認証チェックを追加する。
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // 終了処理（必要であれば実装）
    }
}
