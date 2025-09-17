
package com.example.attendance.dao;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.example.attendance.util.DatabaseUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FaceDataDAO {

    private static final Logger logger = Logger.getLogger(FaceDataDAO.class.getName());

    // 新しい顔データを追加（複数登録対応）
    public void saveFaceData(String username, String faceDescriptorJson, InputStream faceImageInputStream, String label) throws SQLException {
        // 次のface_indexを取得
        int nextFaceIndex = getNextFaceIndex(username);
        
        String sql = "INSERT INTO face_data (username, face_descriptor, face_image, face_index, label) VALUES (?, ?::jsonb, ?, ?, ?)";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, faceDescriptorJson);
            if (faceImageInputStream != null) {
                stmt.setBinaryStream(3, faceImageInputStream);
            } else {
                stmt.setNull(3, java.sql.Types.BINARY);
            }
            stmt.setInt(4, nextFaceIndex);
            stmt.setString(5, label);

            stmt.executeUpdate();
        }
    }

    // 既存の顔データを更新
    public void updateFaceData(String username, int faceIndex, String faceDescriptorJson, InputStream faceImageInputStream, String label) throws SQLException {
        String sql = "UPDATE face_data SET face_descriptor = ?::jsonb, face_image = ?, label = ?, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE username = ? AND face_index = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, faceDescriptorJson);
            if (faceImageInputStream != null) {
                stmt.setBinaryStream(2, faceImageInputStream);
            } else {
                stmt.setNull(2, java.sql.Types.BINARY);
            }
            stmt.setString(3, label);
            stmt.setString(4, username);
            stmt.setInt(5, faceIndex);

            stmt.executeUpdate();
        }
    }

    // 後方互換性のための既存メソッド（最初の顔データを更新または新規作成）
    public void saveFaceData(String username, String faceDescriptorJson, InputStream faceImageInputStream) throws SQLException {
        saveFaceData(username, faceDescriptorJson, faceImageInputStream, "メイン");
    }

    // ユーザーの次のface_indexを取得
    private int getNextFaceIndex(String username) throws SQLException {
        String sql = "SELECT COALESCE(MAX(face_index), 0) + 1 FROM face_data WHERE username = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 1;
    }

    // 全ユーザーの全顔データを取得（Face-API.js形式）
    public String getAllFaceDescriptors() throws SQLException {
        String sql = "SELECT username, face_descriptor, face_index, label FROM face_data ORDER BY username, face_index";
        List<Map<String, Object>> results = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                // usernameがnullまたは空文字の場合はスキップ
                String originalUsername = rs.getString("username");
                if (originalUsername == null || originalUsername.trim().isEmpty()) {
                    logger.warning("無効なusernameを検知しました: nullまたは空文字 - face_index: " + rs.getInt("face_index"));
                    continue;
                }

                Map<String, Object> data = new HashMap<>();
                // ユーザー名にface_indexを含めて一意にする
                String uniqueLabel = originalUsername;
                int faceIndex = rs.getInt("face_index");
                String label = rs.getString("label");

                if (faceIndex > 1) {
                    uniqueLabel += "_" + faceIndex; // 例: "user1_2"
                }

                data.put("username", uniqueLabel);
                data.put("originalUsername", originalUsername); // 元のユーザー名も保持
                data.put("faceIndex", faceIndex);
                data.put("label", label);
                data.put("descriptor", objectMapper.readTree(rs.getString("face_descriptor")));
                results.add(data);
            }

            logger.info("顔データ取得完了: " + results.size() + "件の有効なデータを返却");
        } catch (Exception e) {
            // Handle exception
        }

        try {
            return objectMapper.writeValueAsString(results);
        } catch (Exception e) {
            return "[]";
        }
    }

    // 特定ユーザーの顔データ一覧を取得
    public List<Map<String, Object>> getUserFaceData(String username) throws SQLException {
        String sql = "SELECT id, face_index, label, created_at, updated_at FROM face_data WHERE username = ? ORDER BY face_index";
        List<Map<String, Object>> results = new ArrayList<>();

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", rs.getInt("id"));
                    data.put("faceIndex", rs.getInt("face_index"));
                    data.put("label", rs.getString("label"));
                    data.put("createdAt", rs.getTimestamp("created_at"));
                    data.put("updatedAt", rs.getTimestamp("updated_at"));
                    results.add(data);
                }
            }
        }
        return results;
    }

    // 顔データを削除
    public void deleteFaceData(String username, int faceIndex) throws SQLException {
        String sql = "DELETE FROM face_data WHERE username = ? AND face_index = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setInt(2, faceIndex);
            stmt.executeUpdate();
        }
    }

    public boolean isFaceRegistered(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM face_data WHERE username = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    // ユーザーの顔データ数を取得
    public int getFaceDataCount(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM face_data WHERE username = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
}
