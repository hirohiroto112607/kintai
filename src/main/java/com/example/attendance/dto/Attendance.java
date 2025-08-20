
package com.example.attendance.dto;

import java.time.LocalDateTime;

/**
 * 勤怠情報を保持するデータ転送オブジェクト(DTO)。
 * JavaBeansの規約に従います。
 */
public class Attendance {
    private String userId;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;

    /**
     * デフォルトコンストラクタ。
     */
    public Attendance() {}

    /**
     * ユーザーIDを指定してインスタンスを生成するコンストラクタ。
     * @param userId ユーザーID
     */
    public Attendance(String userId) {
        this.userId = userId;
    }

    // --- Getters and Setters ---

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDateTime getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(LocalDateTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    public LocalDateTime getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(LocalDateTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }
}
