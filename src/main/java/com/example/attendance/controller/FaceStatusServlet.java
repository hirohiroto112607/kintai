
package com.example.attendance.controller;

import com.example.attendance.dao.FaceDataDAO;
import com.example.attendance.dto.User;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/face/status")
public class FaceStatusServlet extends HttpServlet {

    private final FaceDataDAO faceDataDAO = new FaceDataDAO();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "User not authenticated");
            response.getWriter().write(objectMapper.writeValueAsString(error));
            return;
        }

        User user = (User) session.getAttribute("user");
        String username = user.getUsername();

        try {
            boolean isRegistered = faceDataDAO.isFaceRegistered(username);
            Map<String, Object> data = new HashMap<>();
            data.put("isRegistered", isRegistered);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", data);
            response.getWriter().write(objectMapper.writeValueAsString(result));

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to get face status: " + e.getMessage());
            response.getWriter().write(objectMapper.writeValueAsString(error));
        }
    }
}
