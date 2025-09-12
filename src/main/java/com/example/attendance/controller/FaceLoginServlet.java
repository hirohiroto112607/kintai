
package com.example.attendance.controller;

import java.io.IOException;
import java.util.logging.Logger;

import com.example.attendance.dao.UserDAO;
import com.example.attendance.dto.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/face/login")
public class FaceLoginServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger(FaceLoginServlet.class.getName());
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = request.getParameter("username");
        
        logger.info("=== FaceLoginServlet POST request received ===");
        logger.info("Request URI: " + request.getRequestURI());
        logger.info("Context Path: " + request.getContextPath());
        logger.info("Servlet Path: " + request.getServletPath());
        logger.info("Path Info: " + request.getPathInfo());
        logger.info("Query String: " + request.getQueryString());
        logger.info("Method: " + request.getMethod());
        logger.info("Content Type: " + request.getContentType());
        logger.info("Content Length: " + request.getContentLength());
        logger.info("Character Encoding: " + request.getCharacterEncoding());
        
        // All parameters
        logger.info("All parameters:");
        request.getParameterMap().forEach((key, values) -> {
            logger.info("  " + key + " = " + String.join(", ", values));
        });
        
        logger.info("Username parameter: " + (username != null ? "'" + username + "'" : "null"));
        
        if (username != null && !username.isEmpty()) {
            try {
                // 複数顔データ対応: username_faceIndex形式から元のユーザー名を抽出
                String actualUsername = extractOriginalUsername(username);
                logger.info("Original username extracted: " + actualUsername);
                
                logger.info("Attempting to find user: " + actualUsername);
                User user = userDAO.findByUsername(actualUsername);
                if (user != null) {
                    logger.info("User found: " + user.getUsername());
                    HttpSession session = request.getSession();
                    session.setAttribute("user", user);
                    logger.info("Session created with user: " + user.getUsername());
                    response.setStatus(HttpServletResponse.SC_OK);
                } else {
                    logger.warning("User not found: " + actualUsername);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                }
            } catch (Exception e) {
                logger.severe("Error during user lookup: " + e.getMessage());
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            logger.warning("Username parameter is null or empty");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
        
        logger.info("Response status set to: " + response.getStatus());
    }
    
    /**
     * 複数顔データ形式のユーザー名から元のユーザー名を抽出
     * 例: "employee1_2" -> "employee1"
     * 例: "employee1" -> "employee1" (そのまま)
     */
    private String extractOriginalUsername(String username) {
        if (username == null) {
            return null;
        }
        
        // "_数字"の形式で終わっている場合は、それを削除
        int lastUnderscoreIndex = username.lastIndexOf('_');
        if (lastUnderscoreIndex > 0) {
            String suffix = username.substring(lastUnderscoreIndex + 1);
            // 数字のみの場合は face_index とみなす
            if (suffix.matches("\\d+")) {
                return username.substring(0, lastUnderscoreIndex);
            }
        }
        
        // そのまま返す
        return username;
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = request.getParameter("username");

        if (username != null && !username.isEmpty()) {
            try {
                // 複数顔データ対応: username_faceIndex形式から元のユーザー名を抽出
                String actualUsername = extractOriginalUsername(username);
                
                User user = userDAO.findByUsername(actualUsername);
                if (user != null) {
                    HttpSession session = request.getSession();
                    session.setAttribute("user", user);
                    response.sendRedirect(request.getContextPath() + "/attendance");
                } else {
                    response.sendRedirect(request.getContextPath() + "/login.jsp?error=User not found");
                }
            } catch (Exception e) {
                response.sendRedirect(request.getContextPath() + "/login.jsp?error=Login failed");
            }
        } else {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=Invalid request");
        }
    }
}
