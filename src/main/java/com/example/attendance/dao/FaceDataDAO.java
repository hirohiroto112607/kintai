package com.example.attendance.dao;

import com.example.attendance.dto.FaceData;
import com.example.attendance.util.DatabaseUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 顔データDAO
 * face_dataテーブルのCRUD操作を担当
 */
public class FaceDataDAO {
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 顔データを保存（新規登録または更新）
     */
    public boolean save(FaceData faceData) throws SQLException {
        String sql = "INSERT INTO face_data (username, face_descriptor, face_image, confidence_threshold, updated_at) "
                +
                "VALUES (?, ?::jsonb, ?, ?, CURRENT_TIMESTAMP) " +
                "ON CONFLICT (username) " +
                "DO UPDATE SET " +
                "    face_descriptor = EXCLUDED.face_descriptor, " +
                "    face_image = EXCLUDED.face_image, " +
                "    confidence_threshold = EXCLUDED.confidence_threshold, " +
                "    updated_at = CURRENT_TIMESTAMP";

        try (Connection conn = DatabaseUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, faceData.getUsername());
            stmt.setString(2, faceData.getFaceDescriptor());
            stmt.setBytes(3, faceData.getFaceImage());
            stmt.setDouble(4, faceData.getConfidenceThreshold());

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * ユーザー名で顔データを取得
     */
    public Optional<FaceData> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM face_data WHERE username = ?";

        try (Connection conn = DatabaseUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToFaceData(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 全顔データを取得（管理者のみ使用）
     */
    public List<FaceData> findAll() throws SQLException {
        String sql = "SELECT * FROM face_data ORDER BY username";
        List<FaceData> faceDataList = new ArrayList<>();

        try (Connection conn = DatabaseUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                faceDataList.add(mapResultSetToFaceData(rs));
            }
        }
        return faceDataList;
    }

    /**
     * 顔データを削除
     */
    public boolean deleteByUsername(String username) throws SQLException {
        String sql = "DELETE FROM face_data WHERE username = ?";

        try (Connection conn = DatabaseUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * 顔認証用の全データを取得（特徴ベクトルのみ）
     * メモリ効率のため、画像データは含まない
     */
    public List<FaceData> findAllForRecognition() throws SQLException {
        String sql = "SELECT id, username, face_descriptor, confidence_threshold FROM face_data ORDER BY username";
        List<FaceData> faceDataList = new ArrayList<>();

        try (Connection conn = DatabaseUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                FaceData faceData = new FaceData();
                faceData.setId(rs.getInt("id"));
                faceData.setUsername(rs.getString("username"));
                faceData.setFaceDescriptor(rs.getString("face_descriptor"));
                faceData.setConfidenceThreshold(rs.getDouble("confidence_threshold"));
                faceDataList.add(faceData);
            }
        }
        return faceDataList;
    }

    /**
     * ResultSetからFaceDataオブジェクトへのマッピング
     */
    private FaceData mapResultSetToFaceData(ResultSet rs) throws SQLException {
        FaceData faceData = new FaceData();
        faceData.setId(rs.getInt("id"));
        faceData.setUsername(rs.getString("username"));
        faceData.setFaceDescriptor(rs.getString("face_descriptor"));
        faceData.setFaceImage(rs.getBytes("face_image"));
        faceData.setConfidenceThreshold(rs.getDouble("confidence_threshold"));
        faceData.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        faceData.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return faceData;
    }

    /**
     * 顔特徴ベクトルをJSON配列としてパース
     */
    public double[] parseFaceDescriptor(String faceDescriptorJson) {
        if (faceDescriptorJson == null || faceDescriptorJson.trim().isEmpty()) {
            throw new IllegalArgumentException("顔特徴データが空です");
        }

        try {
            // JSON文字列をトリム
            String trimmed = faceDescriptorJson.trim();
            
            // JSON配列の検証
            if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
                throw new IllegalArgumentException("JSON配列形式ではありません");
            }

            List<Double> descriptorList = objectMapper.readValue(
                    trimmed,
                    new TypeReference<List<Double>>() {
                    });

            if (descriptorList.isEmpty()) {
                throw new IllegalArgumentException("顔特徴ベクトルが空です");
            }

            // Face-api.jsの顔特徴ベクトルは通常128次元
            if (descriptorList.size() != 128) {
                throw new IllegalArgumentException(
                    String.format("顔特徴ベクトルの次元数が不正です。期待値: 128, 実際: %d", descriptorList.size()));
            }

            // null値やNaN値のチェック
            for (int i = 0; i < descriptorList.size(); i++) {
                Double value = descriptorList.get(i);
                if (value == null || Double.isNaN(value) || Double.isInfinite(value)) {
                    throw new IllegalArgumentException(
                        String.format("顔特徴ベクトルの%d番目の要素が無効です: %s", i, value));
                }
            }

            return descriptorList.stream().mapToDouble(Double::doubleValue).toArray();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("顔特徴ベクトルのパースに失敗しました: " + e.getMessage(), e);
        }
    }

    /**
     * 顔特徴ベクトルをJSON文字列に変換
     */
    public String serializeFaceDescriptor(double[] descriptor) {
        try {
            return objectMapper.writeValueAsString(descriptor);
        } catch (Exception e) {
            throw new RuntimeException("顔特徴ベクトルのシリアライズに失敗しました", e);
        }
    }
}
