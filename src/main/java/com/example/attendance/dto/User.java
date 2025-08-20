
package com.example.attendance.dto;

/**
 * ユーザー情報を保持するデータ転送オブジェクト(DTO)。
 * JavaBeansの規約に従います。
 */
public class User {
    private String username;
    private String password; // ハッシュ化されたパスワード
    private String role;     // "admin" または "employee"
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
        this(username, password, role, true); // デフォルトで有効
    }

    /**
     * 全てのフィールドを指定してインスタンスを生成するコンストラクタ。
     * @param username ユーザー名
     * @param password ハッシュ化済みパスワード
     * @param role 役割
     * @param enabled アカウントの有効/無効フラグ
     */
    public User(String username, String password, String role, boolean enabled) {
        this.username = username;
        this.password = password;
        this.role = role;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
