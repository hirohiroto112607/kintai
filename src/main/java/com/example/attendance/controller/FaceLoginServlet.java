
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
                logger.info("Attempting to find user: " + username);
                User user = userDAO.findByUsername(username);
                if (user != null) {
                    logger.info("User found: " + user.getUsername());
                    HttpSession session = request.getSession();
                    session.setAttribute("user", user);
                    logger.info("Session created with user: " + user.getUsername());
                    response.setStatus(HttpServletResponse.SC_OK);
                } else {
                    logger.warning("User not found: " + username);
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
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = request.getParameter("username");

        if (username != null && !username.isEmpty()) {
            try {
                User user = userDAO.findByUsername(username);
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
