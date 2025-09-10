package com.example.attendance.dto;

import java.time.LocalDateTime;

/**
 * 顔データDTO
 * face-api.jsで抽出された顔特徴ベクトルと関連情報を保持
 */
public class FaceData {
    private int id;
    private String username;
    private String faceDescriptor; // JSON形式の顔特徴ベクトル
    private byte[] faceImage; // 登録時の顔画像
    private double confidenceThreshold; // 認証時の信頼度閾値
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // デフォルトコンストラクタ
    public FaceData() {
    }

    // コンストラクタ
    public FaceData(String username, String faceDescriptor, byte[] faceImage, double confidenceThreshold) {
        this.username = username;
        this.faceDescriptor = faceDescriptor;
        this.faceImage = faceImage;
        this.confidenceThreshold = confidenceThreshold;
    }

    // Getter/Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFaceDescriptor() {
        return faceDescriptor;
    }

    public void setFaceDescriptor(String faceDescriptor) {
        this.faceDescriptor = faceDescriptor;
    }

    public byte[] getFaceImage() {
        return faceImage;
    }

    public void setFaceImage(byte[] faceImage) {
        this.faceImage = faceImage;
    }

    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public void setConfidenceThreshold(double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "FaceData{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", confidenceThreshold=" + confidenceThreshold +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
