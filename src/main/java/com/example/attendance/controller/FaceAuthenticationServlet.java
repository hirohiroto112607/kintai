
package com.example.attendance.controller;

import com.example.attendance.dao.FaceDataDAO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/face/authenticate")
public class FaceAuthenticationServlet extends HttpServlet {

    private final FaceDataDAO faceDataDAO = new FaceDataDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String faceDescriptorsJson = faceDataDAO.getAllFaceDescriptors();
            request.setAttribute("faceDescriptorsJson", faceDescriptorsJson);
            request.getRequestDispatcher("/jsp/face_authenticate.jsp").forward(request, response);
        } catch (Exception e) {
            // Handle exception
            response.sendRedirect(request.getContextPath() + "/error.jsp");
        }
    }
}
