package com.example.attendance.controller;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.example.attendance.dao.AttendanceDAO;
import com.example.attendance.dao.FaceDataDAO;
import com.example.attendance.dto.FaceData;
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

/**
 * 顔認識機能用サーブレット
 * 顔登録と顔認証のAPIを提供
 */
@WebServlet(urlPatterns = { "/face/register", "/face/authenticate", "/face/status" })
@MultipartConfig(fileSizeThreshold = 1024 * 1024, // 1MB
        maxFileSize = 5 * 1024 * 1024, // 5MB
        maxRequestSize = 10 * 1024 * 1024 // 10MB
)
public class FaceServlet extends HttpServlet {
    private final FaceDataDAO faceDataDAO = new FaceDataDAO();
    private final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();

        if ("/face/status".equals(path)) {
            handleStatusCheck(req, resp);
            return;
        }

        // デフォルトは顔登録ページを表示
        req.getRequestDispatcher("/jsp/face_register.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        resp.setContentType("application/json; charset=UTF-8");

        try {
            // 認証チェック
            User user = getAuthenticatedUser(req);
            if (user == null) {
                sendErrorResponse(resp, "認証が必要です", HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            switch (path) {
                case "/face/register":
                    handleFaceRegistration(req, resp, user);
                    break;
                case "/face/authenticate":
                    handleFaceAuthentication(req, resp, user);
                    break;
                default:
                    sendErrorResponse(resp, "無効なリクエスト", HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (Exception e) {
            getServletContext().log("FaceServlet error", e);
            sendErrorResponse(resp, "サーバーエラーが発生しました: " + e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 顔登録処理
     */
    private void handleFaceRegistration(HttpServletRequest req, HttpServletResponse resp, User user)
            throws IOException, SQLException {
        try {
            // リクエストの詳細ログ
            getServletContext().log("Face registration request for user: " + user.getUsername());
            getServletContext().log("Content-Type: " + req.getContentType());
            
            // リクエストパラメータ取得
            String faceDescriptorJson = req.getParameter("faceDescriptor");
            String confidenceThresholdStr = req.getParameter("confidenceThreshold");

            getServletContext().log("Face descriptor parameter: " + 
                (faceDescriptorJson != null ? "length=" + faceDescriptorJson.length() : "null"));
            getServletContext().log("Confidence threshold parameter: " + confidenceThresholdStr);

            if (faceDescriptorJson == null || faceDescriptorJson.isEmpty()) {
                getServletContext().log("Face descriptor is null or empty");
                sendErrorResponse(resp, "顔特徴データがありません", HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // 顔特徴ベクトルの検証
            double[] descriptor;
            try {
                descriptor = faceDataDAO.parseFaceDescriptor(faceDescriptorJson);
            } catch (Exception e) {
                getServletContext().log("Failed to parse face descriptor: " + e.getMessage());
                sendErrorResponse(resp, "顔特徴データの解析に失敗しました: " + e.getMessage(), HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            if (descriptor.length != 128) {
                getServletContext().log("Invalid descriptor length: " + descriptor.length);
                sendErrorResponse(resp, "無効な顔特徴データです（期待値: 128要素、実際: " + descriptor.length + "要素）", HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // 信頼度閾値の設定（デフォルト0.6）
            double confidenceThreshold = 0.6;
            if (confidenceThresholdStr != null && !confidenceThresholdStr.isEmpty()) {
                try {
                    confidenceThreshold = Double.parseDouble(confidenceThresholdStr);
                    if (confidenceThreshold < 0.0 || confidenceThreshold > 1.0) {
                        confidenceThreshold = 0.6;
                    }
                } catch (NumberFormatException e) {
                    // 無効な値の場合はデフォルトを使用
                }
            }

            // 画像データの取得（オプション）
            byte[] faceImage = null;
            try {
                Part imagePart = req.getPart("faceImage");
                if (imagePart != null && imagePart.getSize() > 0) {
                    getServletContext().log("Face image size: " + imagePart.getSize() + " bytes");
                    try (InputStream inputStream = imagePart.getInputStream()) {
                        faceImage = inputStream.readAllBytes();
                    }
                } else {
                    getServletContext().log("No face image provided");
                }
            } catch (Exception e) {
                getServletContext().log("Error processing face image: " + e.getMessage());
                // 画像の処理エラーは警告として処理し、続行
            }

            // 顔データを保存
            FaceData faceData = new FaceData(user.getUsername(), faceDescriptorJson, faceImage, confidenceThreshold);
            boolean success = faceDataDAO.save(faceData);

            if (success) {
                getServletContext().log("Face registration successful for user: " + user.getUsername());
                sendSuccessResponse(resp, "顔登録が完了しました", null);
            } else {
                getServletContext().log("Face registration failed for user: " + user.getUsername());
                sendErrorResponse(resp, "顔登録に失敗しました", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

        } catch (Exception e) {
            getServletContext().log("Face registration error for user: " + 
                (user != null ? user.getUsername() : "unknown") + " - " + e.getMessage(), e);
            sendErrorResponse(resp, "顔登録処理中にエラーが発生しました: " + e.getMessage(), HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * 顔認証処理
     */
    private void handleFaceAuthentication(HttpServletRequest req, HttpServletResponse resp, User user)
            throws IOException, SQLException {
        try {
            // リクエストパラメータ取得
            String faceDescriptorJson = req.getParameter("faceDescriptor");

            if (faceDescriptorJson == null || faceDescriptorJson.isEmpty()) {
                sendErrorResponse(resp, "顔特徴データがありません", HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // 入力された顔特徴ベクトル
            double[] inputDescriptor = faceDataDAO.parseFaceDescriptor(faceDescriptorJson);
            if (inputDescriptor.length != 128) {
                sendErrorResponse(resp, "無効な顔特徴データです", HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // 全登録ユーザーの顔データを取得
            List<FaceData> allFaceData = faceDataDAO.findAllForRecognition();

            if (allFaceData.isEmpty()) {
                sendErrorResponse(resp, "登録された顔データがありません", HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // 顔認証実行
            FaceRecognitionResult result = performFaceRecognition(inputDescriptor, allFaceData);

            if (result.isRecognized()) {
                // 認証成功：勤怠記録を実行
                String recognizedUsername = result.getRecognizedUsername();
                boolean isCheckIn = shouldCheckIn(recognizedUsername);

                if (isCheckIn) {
                    attendanceDAO.checkIn(recognizedUsername);
                } else {
                    attendanceDAO.checkOut(recognizedUsername);
                }

                sendSuccessResponse(resp, "認証成功: " + (isCheckIn ? "出勤" : "退勤") + "を記録しました", Map.of(
                        "action", isCheckIn ? "check_in" : "check_out",
                        "recognizedUsername", recognizedUsername,
                        "confidence", result.getConfidence(),
                        "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
            } else {
                sendErrorResponse(resp, "顔認証に失敗しました。登録された顔と一致しません", HttpServletResponse.SC_UNAUTHORIZED);
            }

        } catch (Exception e) {
            getServletContext().log("Face authentication error", e);
            sendErrorResponse(resp, "顔認証処理中にエラーが発生しました: " + e.getMessage(), HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * 顔認証ステータスチェック
     */
    private void handleStatusCheck(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = getAuthenticatedUser(req);
        if (user == null) {
            sendErrorResponse(resp, "認証が必要です", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            Optional<FaceData> faceData = faceDataDAO.findByUsername(user.getUsername());
            boolean isRegistered = faceData.isPresent();

            sendSuccessResponse(resp, "ステータス取得成功", Map.of(
                    "isRegistered", isRegistered,
                    "username", user.getUsername(),
                    "role", user.getRole()));

        } catch (SQLException e) {
            getServletContext().log("Status check error", e);
            sendErrorResponse(resp, "ステータス取得に失敗しました", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 顔認識実行
     */
    private FaceRecognitionResult performFaceRecognition(double[] inputDescriptor, List<FaceData> allFaceData) {
        double bestConfidence = 0.0;
        String bestMatchUsername = null;

        for (FaceData faceData : allFaceData) {
            try {
                double[] storedDescriptor = faceDataDAO.parseFaceDescriptor(faceData.getFaceDescriptor());

                // コサイン類似度を計算
                double confidence = calculateCosineSimilarity(inputDescriptor, storedDescriptor);

                // 信頼度閾値チェック
                if (confidence >= faceData.getConfidenceThreshold() && confidence > bestConfidence) {
                    bestConfidence = confidence;
                    bestMatchUsername = faceData.getUsername();
                }
            } catch (Exception e) {
                getServletContext().log("Error parsing face descriptor for user: " + faceData.getUsername(), e);
            }
        }

        return new FaceRecognitionResult(bestMatchUsername, bestConfidence);
    }

    /**
     * コサイン類似度計算
     */
    private double calculateCosineSimilarity(double[] vectorA, double[] vectorB) {
        if (vectorA.length != vectorB.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 出勤/退勤判定
     */
    private boolean shouldCheckIn(String username) {
        if (username == null) {
            return true;
        }
        // 今日の最新の勤怠記録を取得
        List<com.example.attendance.dto.Attendance> todayRecords = attendanceDAO.findByUserIdAndDate(username,
                java.time.LocalDate.now());

        if (todayRecords.isEmpty()) {
            return true; // 今日の記録がない場合は出勤
        }

        // 最新の記録を取得
        com.example.attendance.dto.Attendance latestRecord = todayRecords.get(todayRecords.size() - 1);
        return latestRecord.getCheckOutTime() != null; // 退勤記録がある場合は出勤、ない場合は退勤
    }

    /**
     * 認証済みユーザー取得
     */
    private User getAuthenticatedUser(HttpServletRequest req) {
        // フィルターで設定されたユーザー情報を取得
        User user = (User) req.getAttribute("authenticatedUser");
        if (user == null) {
            // セッションフォールバック
            HttpSession session = req.getSession(false);
            if (session != null) {
                user = (User) session.getAttribute("user");
            }
        }
        return user;
    }

    /**
     * 成功レスポンス送信
     */
    private void sendSuccessResponse(HttpServletResponse resp, String message, Object data) throws IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        objectMapper.writeValue(resp.getWriter(), Map.of(
                "success", true,
                "message", message,
                "data", data));
    }

    /**
     * エラーレスポンス送信
     */
    private void sendErrorResponse(HttpServletResponse resp, String error, int statusCode) throws IOException {
        resp.setStatus(statusCode);
        objectMapper.writeValue(resp.getWriter(), Map.of(
                "success", false,
                "error", error));
    }

    /**
     * 顔認識結果クラス
     */
    private static class FaceRecognitionResult {
        private final String recognizedUsername;
        private final double confidence;

        public FaceRecognitionResult(String recognizedUsername, double confidence) {
            this.recognizedUsername = recognizedUsername;
            this.confidence = confidence;
        }

        public boolean isRecognized() {
            return recognizedUsername != null;
        }

        public String getRecognizedUsername() {
            return recognizedUsername;
        }

        public double getConfidence() {
            return confidence;
        }
    }
}
