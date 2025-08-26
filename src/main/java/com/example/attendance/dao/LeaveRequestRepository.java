package com.example.attendance.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.attendance.util.DatabaseUtil;


public class LeaveRequestRepository {

    public boolean addLeaveRequest(String userId, String leaveType, LocalDate start, LocalDate end, String reason) {
        String sql = "INSERT INTO leave_requests (user_id, leave_type, start_date, end_date, reason, status, applied_at) VALUES (?, ?, ?, ?, ?, 'pending', now())";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setString(2, leaveType);
            stmt.setDate(3, java.sql.Date.valueOf(start));
            stmt.setDate(4, java.sql.Date.valueOf(end));
            stmt.setString(5, reason);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add leave request", e);
        }
    }

    public List<Map<String, Object>> findByUserId(String userId) {
        List<Map<String, Object>> requests = new ArrayList<>();
        String sql = "SELECT l.id, l.leave_type, l.start_date, l.end_date, l.reason, l.status, " +
                     "l.approved_by, l.approval_date, l.created_at " +
                     "FROM leave_requests l WHERE l.user_id = ? ORDER BY l.created_at DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", rs.getInt("id"));
                    map.put("leaveType", rs.getString("leave_type"));
                    map.put("startDate", rs.getDate("start_date").toLocalDate());
                    map.put("endDate", rs.getDate("end_date").toLocalDate());
                    map.put("daysCount", getDaysCount(rs.getDate("start_date").toLocalDate(), rs.getDate("end_date").toLocalDate()));
                    map.put("reason", rs.getString("reason"));
                    map.put("status", rs.getString("status"));
                    map.put("approvedBy", rs.getString("approved_by"));
                    map.put("approvalDate", rs.getTimestamp("approval_date"));
                    map.put("appliedAt", rs.getTimestamp("created_at").toLocalDateTime());

                    // Helper booleans for JSP
                    String status = (String) map.get("status");
                    map.put("isPending", "pending".equals(status));
                    map.put("isApproved", "approved".equals(status));
                    map.put("isRejected", "rejected".equals(status));

                    requests.add(map);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch leave requests by user", e);
        }
        return requests;
    }

    public List<Map<String, Object>> findAllRequests() {
        List<Map<String, Object>> requests = new ArrayList<>();
        String sql = "SELECT l.id, l.user_id, l.leave_type, l.start_date, l.end_date, l.reason, l.status, " +
                     "l.approved_by, l.approval_date, l.created_at, u.username " +
                     "FROM leave_requests l JOIN users u ON l.user_id = u.username " +
                     "ORDER BY l.created_at DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", rs.getInt("id"));
                map.put("userId", rs.getString("user_id"));
                map.put("username", rs.getString("username"));
                map.put("leaveType", rs.getString("leave_type"));
                map.put("startDate", rs.getDate("start_date").toLocalDate());
                map.put("endDate", rs.getDate("end_date").toLocalDate());
                map.put("daysCount", getDaysCount(rs.getDate("start_date").toLocalDate(), rs.getDate("end_date").toLocalDate()));
                map.put("reason", rs.getString("reason"));
                map.put("status", rs.getString("status"));
                map.put("approvedBy", rs.getString("approved_by"));
                map.put("approvalDate", rs.getTimestamp("approval_date"));
                map.put("appliedAt", rs.getTimestamp("created_at").toLocalDateTime());

                // Helper booleans for JSP
                String status = (String) map.get("status");
                map.put("pending", "pending".equalsIgnoreCase(status));
                map.put("approved", "approved".equalsIgnoreCase(status));
                map.put("rejected", "rejected".equalsIgnoreCase(status));

                requests.add(map);
            }
            return requests;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all leave requests", e);
        }
    }

    public List<Map<String, Object>> findPendingRequests() {
        List<Map<String, Object>> requests = new ArrayList<>();
        String sql = "SELECT l.id, l.user_id, l.leave_type, l.start_date, l.end_date, l.reason, l.status, " +
                     "l.approved_by, l.approval_date, l.created_at, u.username " +
                     "FROM leave_requests l JOIN users u ON l.user_id = u.username " +
                     "WHERE l.status = 'pending' ORDER BY l.created_at ASC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", rs.getInt("id"));
                map.put("userId", rs.getString("user_id"));
                map.put("username", rs.getString("username"));
                map.put("leaveType", rs.getString("leave_type"));
                map.put("startDate", rs.getDate("start_date").toLocalDate());
                map.put("endDate", rs.getDate("end_date").toLocalDate());
                map.put("daysCount", getDaysCount(rs.getDate("start_date").toLocalDate(), rs.getDate("end_date").toLocalDate()));
                map.put("reason", rs.getString("reason"));
                map.put("status", rs.getString("status"));
                map.put("approvedBy", rs.getString("approved_by"));
                map.put("approvalDate", rs.getTimestamp("approval_date"));
                map.put("appliedAt", rs.getTimestamp("created_at").toLocalDateTime());

                // Helper booleans for JSP
                String status = (String) map.get("status");
                map.put("pending", "pending".equalsIgnoreCase(status));
                map.put("approved", "approved".equalsIgnoreCase(status));
                map.put("rejected", "rejected".equalsIgnoreCase(status));

                requests.add(map);
            }
            return requests;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch pending leave requests", e);
        }
    }

    public boolean approveRequest(int id, String approverUserId) {
        String sql = "UPDATE leave_requests SET status = 'approved', approved_by = ?, approval_date = now() WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, approverUserId);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error approving leave request: " + e.getMessage());
            throw new RuntimeException("Failed to approve leave request", e);
        }
    }

    public boolean rejectRequest(int id, String approverUserId, String reason) {
        String sql = "UPDATE leave_requests SET status = 'rejected', approved_by = ?, approval_date = now(), rejection_reason = ? WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, approverUserId);
            stmt.setString(2, reason);
            stmt.setInt(3, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to reject leave request", e);
        }
    }

    private Map<String, Object> mapRow(ResultSet rs) throws SQLException {
        Map<String, Object> m = new HashMap<>();
        m.put("id", rs.getInt("id"));
        m.put("userId", rs.getString("user_id"));
        String leaveType = rs.getString("leave_type");
        m.put("leaveType", leaveType);
        m.put("leaveTypeLabel", toLeaveTypeLabel(leaveType));
        LocalDate start = rs.getDate("start_date").toLocalDate();
        LocalDate end = rs.getDate("end_date").toLocalDate();
        m.put("startDate", start);
        m.put("endDate", end);
        m.put("daysCount", getDaysCount(start, end));
        m.put("reason", rs.getString("reason"));
        String status = rs.getString("status");
        m.put("status", status);
        m.put("pending", "pending".equalsIgnoreCase(status));
        m.put("approved", "approved".equalsIgnoreCase(status));
        m.put("rejected", "rejected".equalsIgnoreCase(status));
        m.put("appliedAt", rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp reviewedAt = rs.getTimestamp("approval_date");
        if (reviewedAt != null) m.put("reviewedAt", reviewedAt.toLocalDateTime());
        m.put("rejectionReason", rs.getString("rejection_reason"));
        // inclusive days count
        m.put("daysCount", (int) java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1);
        return m;
    }

    private String toLeaveTypeLabel(String leaveType) {
        if (leaveType == null) return "その他";
        switch (leaveType) {
            case "paid_leave":
                return "年次有給休暇";
            case "sick_leave":
                return "病気休暇";
            case "special_leave":
                return "特別休暇";
            case "other":
            default:
                return "その他";
        }
    }

    public void addLeaveRequest(String userId, String leaveType, String startDate, String endDate, String reason) {
        String sql = "INSERT INTO leave_requests (user_id, leave_type, start_date, end_date, reason) VALUES (?, ?, ?, ?, ?)";

        String mappedLeaveType = mapLeaveType(leaveType);

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setString(2, mappedLeaveType);
            stmt.setDate(3, java.sql.Date.valueOf(startDate));
            stmt.setDate(4, java.sql.Date.valueOf(endDate));
            stmt.setString(5, reason);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add leave request", e);
        }
    }

    private String mapLeaveType(String leaveType) {
        switch (leaveType) {
            case "annual":
                return "paid_leave";
            case "sick":
                return "sick_leave";
            case "personal":
            case "maternity":
            case "paternity":
                return "special_leave";
            default:
                return "other";
        }
    }

    // Add this method to calculate the number of days between two dates (inclusive)
    private int getDaysCount(LocalDate start, LocalDate end) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
    }
}
