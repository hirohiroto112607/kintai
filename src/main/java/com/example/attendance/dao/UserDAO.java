
package com.example.attendance.dao;

import com.example.attendance.dto.User;
import com.example.attendance.util.DatabaseUtil;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * ユーザーデータへのアクセスを担うオブジェクト(DAO)。
 * PostgreSQLデータベースと連携してユーザー情報を管理します。
 */
public class UserDAO {
    
    /**
     * ユーザー名でユーザーを検索します。
     * @param username 検索するユーザー名
     * @return 見つかったUserオブジェクト、見つからない場合はnull
     */
    public User findByUsername(String username) {
        String sql = "SELECT username, password, role, enabled FROM users WHERE username = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role"),
                        rs.getBoolean("enabled")
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by username: " + username, e);
        }
        
        return null;
    }

    /**
     * パスワードを検証します。
     * @param username ユーザー名
     * @param password 検証する平文パスワード
     * @return 検証が成功し、かつアカウントが有効な場合はtrue
     */
    public boolean verifyPassword(String username, String password) {
        User user = findByUsername(username);
        return user != null && user.isEnabled() && user.getPassword().equals(hashPassword(password));
    }

    /**
     * 全てのユーザーを取得します。
     * @return 全ユーザーのコレクション
     */
    public Collection<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT username, password, role, enabled FROM users ORDER BY username";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                users.add(new User(
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("role"),
                    rs.getBoolean("enabled")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get all users", e);
        }
        
        return users;
    }

    /**
     * 新しいユーザーを追加します。
     * @param user 追加するUserオブジェクト
     */
    public void addUser(User user) {
        String sql = "INSERT INTO users (username, password, role, enabled) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getRole());
            stmt.setBoolean(4, user.isEnabled());
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add user: " + user.getUsername(), e);
        }
    }

    /**
     * 既存のユーザー情報を更新します。
     * @param user 更新するUserオブジェクト
     */
    public void updateUser(User user) {
        String sql = "UPDATE users SET password = ?, role = ?, enabled = ? WHERE username = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, user.getPassword());
            stmt.setString(2, user.getRole());
            stmt.setBoolean(3, user.isEnabled());
            stmt.setString(4, user.getUsername());
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update user: " + user.getUsername(), e);
        }
    }

    /**
     * ユーザーを削除します。
     * @param username 削除するユーザー名
     */
    public void deleteUser(String username) {
        String sql = "DELETE FROM users WHERE username = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete user: " + username, e);
        }
    }
    
    /**
     * パスワードをリセットします。
     * @param username 対象のユーザー名
     * @param newPassword 新しい平文パスワード
     */
    public void resetPassword(String username, String newPassword) {
        String sql = "UPDATE users SET password = ? WHERE username = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, hashPassword(newPassword));
            stmt.setString(2, username);
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to reset password for user: " + username, e);
        }
    }

    /**
     * ユーザーアカウントの有効/無効を切り替えます。
     * @param username 対象のユーザー名
     * @param enabled 有効にする場合はtrue, 無効にする場合はfalse
     */
    public void toggleUserEnabled(String username, boolean enabled) {
        String sql = "UPDATE users SET enabled = ? WHERE username = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setBoolean(1, enabled);
            stmt.setString(2, username);
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to toggle user enabled status: " + username, e);
        }
    }

    /**
     * 平文のパスワードをSHA-256でハッシュ化します。
     * @param password ハッシュ化する平文パスワード
     * @return ハッシュ化された16進文字列
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // 本来はロギングすべき
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
