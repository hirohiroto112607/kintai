
package com.example.attendance.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.example.attendance.dao.FaceDataDAO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/face/authenticate")
public class FaceAuthenticationServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger(FaceAuthenticationServlet.class.getName());
    private final FaceDataDAO faceDataDAO = new FaceDataDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.info("Face authentication request received");
        
        try {
            // データベース接続とテーブル存在の確認
            logger.info("Attempting to retrieve face descriptors from database");
            String faceDescriptorsJson = faceDataDAO.getAllFaceDescriptors();
            
            if (faceDescriptorsJson != null) {
                final String finalJson = faceDescriptorsJson;
                logger.info(() -> "Face descriptors retrieved successfully: " + finalJson.length() + " characters");
            } else {
                logger.warning("Face descriptors is null, setting to empty array");
                faceDescriptorsJson = "[]";
            }
            
            request.setAttribute("faceDescriptorsJson", faceDescriptorsJson);
            
            logger.info("Forwarding to face_authenticate.jsp");
            request.getRequestDispatcher("/jsp/face_authenticate.jsp").forward(request, response);
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error in face authentication servlet", e);
            
            String errorMessage = "データベースエラーが発生しました。face_dataテーブルが存在するかご確認ください。エラー: " + e.getMessage();
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=face_auth_failed&errorMessage=" + 
                                 java.net.URLEncoder.encode(errorMessage, "UTF-8"));
        } catch (ServletException | IOException e) {
            logger.log(Level.SEVERE, "Servlet error in face authentication", e);
            throw e; // サーブレット例外は再スロー
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Runtime error in face authentication servlet", e);
            
            String errorMessage = "顔認証ページの読み込みでエラーが発生しました: " + e.getMessage();
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=face_auth_failed&errorMessage=" + 
                                 java.net.URLEncoder.encode(errorMessage, "UTF-8"));
        }
    }
}
