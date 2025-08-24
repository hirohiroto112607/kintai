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
