package com.example.attendance.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.example.attendance.dto.Department;
import com.example.attendance.util.DatabaseUtil;

/**
 * 部署データへのアクセスを担うオブジェクト(DAO)。
 * PostgreSQLデータベースと連携して部署情報を管理します。
 */
public class DepartmentDAO {

    /**
     * 全ての部署を取得します。
     * @return 全部署のコレクション
     */
    public Collection<Department> getAllDepartments() {
        List<Department> departments = new ArrayList<>();
        String sql = "SELECT department_id, department_name, description, enabled FROM departments ORDER BY department_name";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                departments.add(new Department(
                    rs.getString("department_id"),
                    rs.getString("department_name"),
                    rs.getString("description"),
                    rs.getBoolean("enabled")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get all departments", e);
        }
        
        return departments;
    }


    /**
     * 有効な部署のみを取得します。
     * @return 有効な部署のコレクション
     */
    public Collection<Department> getEnabledDepartments() {
        List<Department> departments = new ArrayList<>();
        String sql = "SELECT department_id, department_name, description, enabled FROM departments WHERE enabled = true ORDER BY department_name";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                departments.add(new Department(
                    rs.getString("department_id"),
                    rs.getString("department_name"),
                    rs.getString("description"),
                    rs.getBoolean("enabled")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get enabled departments", e);
        }
        
        return departments;
    }

    /**
     * 部署IDで部署を検索します。
     * @param departmentId 検索する部署ID
     * @return 見つかった部署、見つからない場合はnull
     */
    public Department findByDepartmentId(String departmentId) {
        String sql = "SELECT department_id, department_name, description, enabled FROM departments WHERE department_id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, departmentId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Department(
                        rs.getString("department_id"),
                        rs.getString("department_name"),
                        rs.getString("description"),
                        rs.getBoolean("enabled")
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find department by ID: " + departmentId, e);
        }
        
        return null;
    }

    /**
     * 新しい部署を追加します。
     * @param department 追加するDepartmentオブジェクト
     */
    public void addDepartment(Department department) {
        String sql = "INSERT INTO departments (department_id, department_name, description, enabled) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, department.getDepartmentId());
            stmt.setString(2, department.getDepartmentName());
            stmt.setString(3, department.getDescription());
            stmt.setBoolean(4, department.isEnabled());
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add department: " + department.getDepartmentId(), e);
        }
    }

    /**
     * 既存の部署情報を更新します。
     * @param department 更新するDepartmentオブジェクト
     */
    public void updateDepartment(Department department) {
        String sql = "UPDATE departments SET department_name = ?, description = ?, enabled = ? WHERE department_id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, department.getDepartmentName());
            stmt.setString(2, department.getDescription());
            stmt.setBoolean(3, department.isEnabled());
            stmt.setString(4, department.getDepartmentId());
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update department: " + department.getDepartmentId(), e);
        }
    }

    /**
     * 部署を削除します。
     * @param departmentId 削除する部署ID
     */
    public void deleteDepartment(String departmentId) {
        String sql = "DELETE FROM departments WHERE department_id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, departmentId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete department: " + departmentId, e);
        }
    }
}

/**
 * 休暇申請データへのアクセスを担うオブジェクト(DAO)。
 * PostgreSQLデータベースと連携して休暇申請情報を管理します。
 */
class LeaveRequestDAO {

    /**
     * 新しい休暇申請を追加します。
     */
    public boolean addLeaveRequest(Object leaveRequest) {
        String sql = "INSERT INTO leave_requests (user_id, leave_type, start_date, end_date, reason, status, applied_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Reflection を使用してフィールドアクセス（簡略化のため）
            Class<?> clazz = leaveRequest.getClass();
            stmt.setString(1, (String) clazz.getMethod("getUserId").invoke(leaveRequest));
            stmt.setString(2, (String) clazz.getMethod("getLeaveType").invoke(leaveRequest));
            stmt.setDate(3, java.sql.Date.valueOf((java.time.LocalDate) clazz.getMethod("getStartDate").invoke(leaveRequest)));
            stmt.setDate(4, java.sql.Date.valueOf((java.time.LocalDate) clazz.getMethod("getEndDate").invoke(leaveRequest)));
            stmt.setString(5, (String) clazz.getMethod("getReason").invoke(leaveRequest));
            stmt.setString(6, (String) clazz.getMethod("getStatus").invoke(leaveRequest));
            stmt.setTimestamp(7, java.sql.Timestamp.valueOf((java.time.LocalDateTime) clazz.getMethod("getAppliedAt").invoke(leaveRequest)));
            
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            throw new RuntimeException("休暇申請の追加に失敗しました", e);
        }
    }

    /**
     * 指定されたユーザーの休暇申請一覧を取得します。
     */
    public List<Object> findByUserId(String userId) {
        List<Object> requests = new ArrayList<>();
        String sql = "SELECT * FROM leave_requests WHERE user_id = ? ORDER BY applied_at DESC";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                requests.add(mapResultSetToLeaveRequest(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("休暇申請の取得に失敗しました", e);
        }
        return requests;
    }

    /**
     * 全ての休暇申請一覧を取得します（管理者用）。
     */
    public List<Object> findAllRequests() {
        List<Object> requests = new ArrayList<>();
        String sql = "SELECT * FROM leave_requests ORDER BY applied_at DESC";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                requests.add(mapResultSetToLeaveRequest(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("休暇申請一覧の取得に失敗しました", e);
        }
        return requests;
    }

    /**
     * 承認待ちの休暇申請一覧を取得します。
     */
    public List<Object> findPendingRequests() {
        List<Object> requests = new ArrayList<>();
        String sql = "SELECT * FROM leave_requests WHERE status = 'PENDING' ORDER BY applied_at ASC";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                requests.add(mapResultSetToLeaveRequest(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("承認待ち休暇申請の取得に失敗しました", e);
        }
        return requests;
    }

    /**
     * 休暇申請を承認します。
     */
    public boolean approveRequest(Integer requestId, String approverUserId) {
        String sql = "UPDATE leave_requests SET status = 'APPROVED', approver_user_id = ?, reviewed_at = ? WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, approverUserId);
            stmt.setTimestamp(2, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
            stmt.setInt(3, requestId);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("休暇申請の承認に失敗しました", e);
        }
    }

    /**
     * 休暇申請を却下します。
     */
    public boolean rejectRequest(Integer requestId, String approverUserId, String rejectionReason) {
        String sql = "UPDATE leave_requests SET status = 'REJECTED', approver_user_id = ?, reviewed_at = ?, rejection_reason = ? WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, approverUserId);
            stmt.setTimestamp(2, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
            stmt.setString(3, rejectionReason);
            stmt.setInt(4, requestId);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("休暇申請の却下に失敗しました", e);
        }
    }

    /**
     * IDで休暇申請を取得します。
     */
    public Object findById(Integer id) {
        String sql = "SELECT * FROM leave_requests WHERE id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToLeaveRequest(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("休暇申請の取得に失敗しました", e);
        }
        return null;
    }

    /**
     * ResultSetからLeaveRequestオブジェクトにマッピングします。
     */
    private Object mapResultSetToLeaveRequest(ResultSet rs) throws SQLException {
        // 簡略化のため、Mapで返却
        java.util.Map<String, Object> request = new java.util.HashMap<>();
        request.put("id", rs.getInt("id"));
        request.put("userId", rs.getString("user_id"));
        request.put("leaveType", rs.getString("leave_type"));
        request.put("startDate", rs.getDate("start_date").toLocalDate());
        request.put("endDate", rs.getDate("end_date").toLocalDate());
        request.put("reason", rs.getString("reason"));
        request.put("status", rs.getString("status"));
        request.put("approverUserId", rs.getString("approver_user_id"));
        request.put("appliedAt", rs.getTimestamp("applied_at").toLocalDateTime());
        
        java.sql.Timestamp reviewedAtTs = rs.getTimestamp("reviewed_at");
        if (reviewedAtTs != null) {
            request.put("reviewedAt", reviewedAtTs.toLocalDateTime());
        }
        
        request.put("rejectionReason", rs.getString("rejection_reason"));
        return request;
    }
}
