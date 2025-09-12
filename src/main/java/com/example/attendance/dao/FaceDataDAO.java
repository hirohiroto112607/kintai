
package com.example.attendance.dao;

import com.example.attendance.util.DatabaseUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FaceDataDAO {

    public void saveFaceData(String username, String faceDescriptorJson, InputStream faceImageInputStream) throws SQLException {
        String sql = "INSERT INTO face_data (username, face_descriptor, face_image) VALUES (?, ?::jsonb, ?) " +
                     "ON CONFLICT (username) DO UPDATE SET face_descriptor = EXCLUDED.face_descriptor, face_image = EXCLUDED.face_image, updated_at = CURRENT_TIMESTAMP";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, faceDescriptorJson);
            if (faceImageInputStream != null) {
                stmt.setBinaryStream(3, faceImageInputStream);
            } else {
                stmt.setNull(3, java.sql.Types.BINARY);
            }

            stmt.executeUpdate();
        }
    }

    public String getAllFaceDescriptors() throws SQLException {
        String sql = "SELECT username, face_descriptor FROM face_data";
        List<Map<String, Object>> results = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> data = new HashMap<>();
                data.put("username", rs.getString("username"));
                // The face_descriptor is stored as JSONB, so we get it as a string
                // and the client-side JavaScript will parse it.
                data.put("descriptor", objectMapper.readTree(rs.getString("face_descriptor")));
                results.add(data);
            }
        } catch (Exception e) {
            // Handle exception
        }

        try {
            return objectMapper.writeValueAsString(results);
        } catch (Exception e) {
            return "[]";
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
}
