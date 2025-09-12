
package com.example.attendance.controller;

import com.example.attendance.dao.FaceDataDAO;
import com.example.attendance.dto.User;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/face/register")
@MultipartConfig
public class FaceRegistrationServlet extends HttpServlet {

    private final FaceDataDAO faceDataDAO = new FaceDataDAO();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
            Part faceDescriptorPart = request.getPart("faceDescriptor");
            String faceDescriptorJson = new String(faceDescriptorPart.getInputStream().readAllBytes(), "UTF-8");

            Part faceImagePart = request.getPart("faceImage");
            InputStream faceImageInputStream = null;
            if (faceImagePart != null) {
                faceImageInputStream = faceImagePart.getInputStream();
            }

            faceDataDAO.saveFaceData(username, faceDescriptorJson, faceImageInputStream);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            response.getWriter().write(objectMapper.writeValueAsString(result));

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to register face: " + e.getMessage());
            response.getWriter().write(objectMapper.writeValueAsString(error));
        }
    }
}
