package com.example.attendance.dto;

/**
 * 部署情報を保持するデータ転送オブジェクト(DTO)。
 * JavaBeansの規約に従います。
 */
public class Department {
    private String departmentId;
    private String departmentName;
    private String description;
    private boolean enabled;

    /**
     * デフォルトコンストラクタ。
     */
    public Department() {}

    /**
     * 全てのフィールドを指定してインスタンスを生成するコンストラクタ。
     * @param departmentId 部署ID
     * @param departmentName 部署名
     * @param description 部署説明
     * @param enabled 有効フラグ
     */
    public Department(String departmentId, String departmentName, String description, boolean enabled) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.description = description;
        this.enabled = enabled;
    }

    // --- Getters and Setters ---

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

/**
 * 休暇申請情報を保持するデータ転送オブジェクト(DTO)。
 * JavaBeansの規約に従います。
 */
class LeaveRequest {
    private Integer id;
    private String userId;
    private String leaveType;
    private java.time.LocalDate startDate;
    private java.time.LocalDate endDate;
    private String reason;
    private String status;
    private String approverUserId;
    private java.time.LocalDateTime appliedAt;
    private java.time.LocalDateTime reviewedAt;
    private String rejectionReason;

    // デフォルトコンストラクタ
    public LeaveRequest() {}

    // 全パラメータコンストラクタ
    public LeaveRequest(String userId, String leaveType, java.time.LocalDate startDate, 
                       java.time.LocalDate endDate, String reason) {
        this.userId = userId;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.status = "PENDING";
        this.appliedAt = java.time.LocalDateTime.now();
    }

    // ゲッターとセッター
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

    public java.time.LocalDate getStartDate() { return startDate; }
    public void setStartDate(java.time.LocalDate startDate) { this.startDate = startDate; }

    public java.time.LocalDate getEndDate() { return endDate; }
    public void setEndDate(java.time.LocalDate endDate) { this.endDate = endDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getApproverUserId() { return approverUserId; }
    public void setApproverUserId(String approverUserId) { this.approverUserId = approverUserId; }

    public java.time.LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(java.time.LocalDateTime appliedAt) { this.appliedAt = appliedAt; }

    public java.time.LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(java.time.LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    // ヘルパーメソッド
    public boolean isPending() { return "PENDING".equals(status); }
    public boolean isApproved() { return "APPROVED".equals(status); }
    public boolean isRejected() { return "REJECTED".equals(status); }
    
    public int getDaysCount() {
        if (startDate == null || endDate == null) return 0;
        return (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }
}
