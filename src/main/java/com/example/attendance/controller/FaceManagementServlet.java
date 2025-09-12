package com.example.attendance.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.example.attendance.dao.FaceDataDAO;
import com.example.attendance.dto.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/face/manage")
public class FaceManagementServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger(FaceManagementServlet.class.getName());
    private final FaceDataDAO faceDataDAO = new FaceDataDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) {
            user = (User) request.getSession().getAttribute("user");
        }
        
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        try {
            // ユーザーの既存の顔データ一覧を取得
            List<Map<String, Object>> faceDataList = faceDataDAO.getUserFaceData(user.getUsername());
            request.setAttribute("faceDataList", faceDataList);
            request.setAttribute("username", user.getUsername());
            
            logger.info("Face management page accessed by user: " + user.getUsername());
            logger.info("Existing face data count: " + faceDataList.size());
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error in face management servlet", e);
            request.setAttribute("errorMessage", "データベースエラーが発生しました: " + e.getMessage());
        }

        request.getRequestDispatcher("/jsp/face_management.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) {
            user = (User) request.getSession().getAttribute("user");
        }
        
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String action = request.getParameter("action");
        
        try {
            if ("delete".equals(action)) {
                int faceIndex = Integer.parseInt(request.getParameter("faceIndex"));
                faceDataDAO.deleteFaceData(user.getUsername(), faceIndex);
                
                logger.info("Face data deleted: user=" + user.getUsername() + ", faceIndex=" + faceIndex);
                request.setAttribute("successMessage", "顔データを削除しました。");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error processing face management action", e);
            request.setAttribute("errorMessage", "処理中にエラーが発生しました: " + e.getMessage());
        } catch (NumberFormatException e) {
            request.setAttribute("errorMessage", "無効なパラメータです。");
        }

        // 処理後にGETと同じ処理を実行
        doGet(request, response);
    }
}