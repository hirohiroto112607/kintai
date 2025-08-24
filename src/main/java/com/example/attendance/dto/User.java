
package com.example.attendance.dto;

/**
 * ユーザー情報を保持するデータ転送オブジェクト(DTO)。
 * JavaBeansの規約に従います。
 */
public class User {
    private String username;
    private String password; // ハッシュ化されたパスワード
    private String role;     // "admin" または "employee"
    private String departmentId; // 部署ID
    private boolean enabled; // アカウントが有効か無効か

    /**
     * デフォルトコンストラクタ。
     */
    public User() {}
    
    /**
     * 主要なフィールドを指定してインスタンスを生成するコンストラクタ。
     * アカウントはデフォルトで有効になります。
     * @param username ユーザー名
     * @param password ハッシュ化済みパスワード
     * @param role 役割
     */
    public User(String username, String password, String role) {
        this(username, password, role, null, true); // デフォルトで有効、部署なし
    }

    /**
     * 部署IDを含む主要なフィールドを指定してインスタンスを生成するコンストラクタ。
     * @param username ユーザー名
     * @param password ハッシュ化済みパスワード
     * @param role 役割
     * @param departmentId 部署ID
     */
    public User(String username, String password, String role, String departmentId) {
        this(username, password, role, departmentId, true); // デフォルトで有効
    }

    /**
     * 全てのフィールドを指定してインスタンスを生成するコンストラクタ。
     * @param username ユーザー名
     * @param password ハッシュ化済みパスワード
     * @param role 役割
     * @param departmentId 部署ID
     * @param enabled アカウントの有効/無効フラグ
     */
    public User(String username, String password, String role, String departmentId, boolean enabled) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.departmentId = departmentId;
        this.enabled = enabled;
    }

    // --- Getters and Setters ---

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
