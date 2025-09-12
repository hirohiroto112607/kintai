
package com.example.attendance.controller;

import com.example.attendance.dao.UserDAO;
import com.example.attendance.dto.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/face/login")
public class FaceLoginServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = request.getParameter("username");

        if (username != null && !username.isEmpty()) {
            try {
                User user = userDAO.findByUsername(username);
                if (user != null) {
                    HttpSession session = request.getSession();
                    session.setAttribute("user", user);
                    response.setStatus(HttpServletResponse.SC_OK);
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                }
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
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
