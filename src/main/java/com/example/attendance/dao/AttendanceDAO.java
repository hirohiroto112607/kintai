
package com.example.attendance.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.attendance.dto.Attendance;
import com.example.attendance.util.DatabaseUtil;

/**
 * 勤怠データへのアクセスを担うオブジェクト(DAO)。
 * PostgreSQLデータベースと連携して勤怠情報を管理します。
 */
public class AttendanceDAO {

    /**
     * 出勤を記録します。
     * @param userId ユーザーID
     * @return 成功した場合true、失敗した場合false
     */
    public boolean checkIn(String userId) {
        String sql = "INSERT INTO attendance (user_id, check_in_time) VALUES (?, ?)";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, userId);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check in for user: " + userId, e);
        }
    }

    /**
     * 退勤を記録します。
     * 指定されたユーザーの最新の未退勤レコードに退勤時刻を設定します。
     * @param userId ユーザーID
     * @return 成功した場合true、失敗した場合false
     */
    public boolean checkOut(String userId) {
        String sql = "UPDATE attendance SET check_out_time = ? " +
                    "WHERE user_id = ? AND check_out_time IS NULL " +
                    "AND id = (SELECT id FROM attendance WHERE user_id = ? AND check_out_time IS NULL " +
                    "ORDER BY check_in_time DESC LIMIT 1)";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(2, userId);
            stmt.setString(3, userId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check out for user: " + userId, e);
        }
    }

    /**
     * 指定されたユーザーの最新勤怠状況を確認します。
     * @param userId ユーザーID
     * @return 出勤中の場合はtrue、未出勤または退勤済みの場合はfalse
     */
    public boolean isCurrentlyCheckedIn(String userId) {
        String sql = "SELECT COUNT(*) FROM attendance WHERE user_id = ? AND check_out_time IS NULL";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check attendance status for user: " + userId, e);
        }
        
        return false;
    }

    /**
     * 指定されたユーザーの全勤怠履歴を取得します。
     * @param userId ユーザーID
     * @return 勤怠記録のリスト
     */
    public List<Attendance> findByUserId(String userId) {
        List<Attendance> attendances = new ArrayList<>();
        String sql = "SELECT id, user_id, check_in_time, check_out_time FROM attendance " +
                    "WHERE user_id = ? ORDER BY check_in_time DESC";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Attendance attendance = new Attendance(rs.getString("user_id"));
                    attendance.setCheckInTime(rs.getTimestamp("check_in_time").toLocalDateTime());
                    
                    Timestamp checkOutTimestamp = rs.getTimestamp("check_out_time");
                    if (checkOutTimestamp != null) {
                        attendance.setCheckOutTime(checkOutTimestamp.toLocalDateTime());
                    }
                    
                    attendances.add(attendance);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find attendance records for user: " + userId, e);
        }
        
        return attendances;
    }

    /**
     * 指定されたユーザーの指定日の勤怠履歴を取得します。
     * @param userId ユーザーID
     * @param date 検索対象の日付
     * @return 指定日の勤怠記録のリスト
     */
    public List<Attendance> findByUserIdAndDate(String userId, LocalDate date) {
        List<Attendance> attendances = new ArrayList<>();
        String sql = "SELECT id, user_id, check_in_time, check_out_time FROM attendance " +
                    "WHERE user_id = ? AND DATE(check_in_time) = ? ORDER BY check_in_time ASC";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, userId);
            stmt.setDate(2, Date.valueOf(date));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Attendance attendance = new Attendance(rs.getString("user_id"));
                    attendance.setCheckInTime(rs.getTimestamp("check_in_time").toLocalDateTime());
                    
                    Timestamp checkOutTimestamp = rs.getTimestamp("check_out_time");
                    if (checkOutTimestamp != null) {
                        attendance.setCheckOutTime(checkOutTimestamp.toLocalDateTime());
                    }
                    
                    attendances.add(attendance);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find attendance records for user: " + userId + " on date: " + date, e);
        }
        
        return attendances;
    }

    /**
     * 全ての勤怠履歴を取得します。
     * @return 全勤怠記録のリスト
     */
    public List<Attendance> findAll() {
        List<Attendance> attendances = new ArrayList<>();
        String sql = "SELECT id, user_id, check_in_time, check_out_time FROM attendance " +
                    "ORDER BY check_in_time DESC";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Attendance attendance = new Attendance(rs.getString("user_id"));
                attendance.setCheckInTime(rs.getTimestamp("check_in_time").toLocalDateTime());
                
                Timestamp checkOutTimestamp = rs.getTimestamp("check_out_time");
                if (checkOutTimestamp != null) {
                    attendance.setCheckOutTime(checkOutTimestamp.toLocalDateTime());
                }
                
                attendances.add(attendance);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find all attendance records", e);
        }
        
        return attendances;
    }

    /**
     * 指定された条件で勤怠履歴を絞り込み検索します。
     * @param userId ユーザーID (nullまたは空文字の場合は全ユーザー)
     * @param startDate 開始日 (nullの場合は指定なし)
     * @param endDate 終了日 (nullの場合は指定なし)
     * @return 絞り込まれた勤怠記録のリスト
     */
    public List<Attendance> findFilteredRecords(String userId, LocalDate startDate, LocalDate endDate) {
        List<Attendance> attendances = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT id, user_id, check_in_time, check_out_time FROM attendance WHERE 1=1");
        List<Object> parameters = new ArrayList<>();
        
        if (userId != null && !userId.isEmpty()) {
            sql.append(" AND user_id = ?");
            parameters.add(userId);
        }
        
        if (startDate != null) {
            sql.append(" AND DATE(check_in_time) >= ?");
            parameters.add(Date.valueOf(startDate));
        }
        
        if (endDate != null) {
            sql.append(" AND DATE(check_in_time) <= ?");
            parameters.add(Date.valueOf(endDate));
        }
        
        sql.append(" ORDER BY check_in_time DESC");
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < parameters.size(); i++) {
                if (parameters.get(i) instanceof String) {
                    stmt.setString(i + 1, (String) parameters.get(i));
                } else if (parameters.get(i) instanceof Date) {
                    stmt.setDate(i + 1, (Date) parameters.get(i));
                }
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Attendance attendance = new Attendance(rs.getString("user_id"));
                    attendance.setCheckInTime(rs.getTimestamp("check_in_time").toLocalDateTime());
                    
                    Timestamp checkOutTimestamp = rs.getTimestamp("check_out_time");
                    if (checkOutTimestamp != null) {
                        attendance.setCheckOutTime(checkOutTimestamp.toLocalDateTime());
                    }
                    
                    attendances.add(attendance);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find filtered attendance records", e);
        }
        
        return attendances;
    }

    /**
     * 月別の合計労働時間を計算します。
     * @param userId ユーザーID (nullまたは空文字の場合は全ユーザー)
     * @return 月(YearMonth)をキー、合計労働時間(時)を値とするマップ
     */
    public Map<YearMonth, Long> getMonthlyWorkingHours(String userId) {
        Map<YearMonth, Long> monthlyHours = new HashMap<>();
        StringBuilder sql = new StringBuilder(
            "SELECT DATE_TRUNC('month', check_in_time) as month, " +
            "SUM(EXTRACT(EPOCH FROM (check_out_time - check_in_time))/3600) as total_hours " +
            "FROM attendance WHERE check_out_time IS NOT NULL"
        );
        
        if (userId != null && !userId.isEmpty()) {
            sql.append(" AND user_id = ?");
        }
        
        sql.append(" GROUP BY DATE_TRUNC('month', check_in_time) ORDER BY month");
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            if (userId != null && !userId.isEmpty()) {
                stmt.setString(1, userId);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp monthTimestamp = rs.getTimestamp("month");
                    YearMonth yearMonth = YearMonth.from(monthTimestamp.toLocalDateTime());
                    Long totalHours = Math.round(rs.getDouble("total_hours"));
                    monthlyHours.put(yearMonth, totalHours);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to calculate monthly working hours", e);
        }
        
        return monthlyHours;
    }

    /**
     * 月別の出勤日数を計算します。
     * @param userId ユーザーID (nullまたは空文字の場合は全ユーザー)
     * @return 月(YearMonth)をキー、出勤日数を値とするマップ
     */
    public Map<YearMonth, Long> getMonthlyCheckInCounts(String userId) {
        Map<YearMonth, Long> monthlyCounts = new HashMap<>();
        StringBuilder sql = new StringBuilder(
            "SELECT DATE_TRUNC('month', check_in_time) as month, COUNT(*) as check_in_count " +
            "FROM attendance WHERE 1=1"
        );
        
        if (userId != null && !userId.isEmpty()) {
            sql.append(" AND user_id = ?");
        }
        
        sql.append(" GROUP BY DATE_TRUNC('month', check_in_time) ORDER BY month");
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            if (userId != null && !userId.isEmpty()) {
                stmt.setString(1, userId);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp monthTimestamp = rs.getTimestamp("month");
                    YearMonth yearMonth = YearMonth.from(monthTimestamp.toLocalDateTime());
                    Long count = rs.getLong("check_in_count");
                    monthlyCounts.put(yearMonth, count);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to calculate monthly check-in counts", e);
        }
        
        return monthlyCounts;
    }

    /**
     * 管理者が手動で勤怠記録を追加します。
     * @param userId ユーザーID
     * @param checkIn 出勤日時
     * @param checkOut 退勤日時 (null許容)
     */
    public void addManualAttendance(String userId, LocalDateTime checkIn, LocalDateTime checkOut) {
        String sql = "INSERT INTO attendance (user_id, check_in_time, check_out_time) VALUES (?, ?, ?)";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, userId);
            stmt.setTimestamp(2, Timestamp.valueOf(checkIn));
            stmt.setTimestamp(3, checkOut != null ? Timestamp.valueOf(checkOut) : null);
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add manual attendance record", e);
        }
    }

    /**
     * 管理者が手動で勤怠記録を更新します。
     * @param userId ユーザーID
     * @param oldCheckIn 更新前の出勤日時
     * @param oldCheckOut 更新前の退勤日時
     * @param newCheckIn 更新後の出勤日時
     * @param newCheckOut 更新後の退勤日時
     * @return 更新が成功した場合はtrue
     */
    public boolean updateManualAttendance(String userId, LocalDateTime oldCheckIn, LocalDateTime oldCheckOut, 
                                        LocalDateTime newCheckIn, LocalDateTime newCheckOut) {
        String sql = "UPDATE attendance SET check_in_time = ?, check_out_time = ? " +
                    "WHERE user_id = ? AND check_in_time = ? AND " +
                    "(check_out_time = ? OR (check_out_time IS NULL AND ? IS NULL))";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setTimestamp(1, Timestamp.valueOf(newCheckIn));
            stmt.setTimestamp(2, newCheckOut != null ? Timestamp.valueOf(newCheckOut) : null);
            stmt.setString(3, userId);
            stmt.setTimestamp(4, Timestamp.valueOf(oldCheckIn));
            stmt.setTimestamp(5, oldCheckOut != null ? Timestamp.valueOf(oldCheckOut) : null);
            stmt.setTimestamp(6, oldCheckOut != null ? Timestamp.valueOf(oldCheckOut) : null);
            
            int updatedRows = stmt.executeUpdate();
            return updatedRows > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update manual attendance record", e);
        }
    }

    /**
     * 管理者が手動で勤怠記録を削除します。
     * @param userId ユーザーID
     * @param checkIn 削除対象の出勤日時
     * @param checkOut 削除対象の退勤日時
     * @return 削除が成功した場合はtrue
     */
    public boolean deleteManualAttendance(String userId, LocalDateTime checkIn, LocalDateTime checkOut) {
        String sql = "DELETE FROM attendance WHERE user_id = ? AND check_in_time = ? AND " +
                    "(check_out_time = ? OR (check_out_time IS NULL AND ? IS NULL))";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, userId);
            stmt.setTimestamp(2, Timestamp.valueOf(checkIn));
            stmt.setTimestamp(3, checkOut != null ? Timestamp.valueOf(checkOut) : null);
            stmt.setTimestamp(4, checkOut != null ? Timestamp.valueOf(checkOut) : null);
            
            int deletedRows = stmt.executeUpdate();
            return deletedRows > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete manual attendance record", e);
        }
    }
}
