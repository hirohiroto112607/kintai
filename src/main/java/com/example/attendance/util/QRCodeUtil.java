package com.example.attendance.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * QRコード生成とトークン管理のユーティリティクラス
 */
public class QRCodeUtil {
    
    // QRコードトークンの有効期限（分）
    private static final int TOKEN_VALIDITY_MINUTES = 5;
    
    // トークンストレージ（本番環境ではRedisなど永続ストレージを使用すること）
    private static final Map<String, TokenData> tokenStorage = new ConcurrentHashMap<>();
    
    private static final SecureRandom random = new SecureRandom();

    /**
     * QRコード用の一意トークンを生成する
     * @param action アクション（check_in または check_out）
     * @return 生成されたトークン
     */
    public static String generateToken(String action) {
        // 32文字のランダムトークンを生成
        byte[] tokenBytes = new byte[24];
        random.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        
        // トークンデータを保存
        TokenData tokenData = new TokenData(action, LocalDateTime.now());
        tokenStorage.put(token, tokenData);
        
        // 古いトークンのクリーンアップ
        cleanupExpiredTokens();
        
        return token;
    }

    /**
     * ユーザーID用のQRコードを生成する（ユーザーIDをそのまま返す）
     * @param userId ユーザーID
     * @return ユーザーID
     */
    public static String generateUserIdQRCode(String userId) {
        return userId;
    }
    
    /**
     * トークンを検証し、有効な場合はアクションを返す
     * @param token 検証するトークン
     * @return 有効な場合はアクション、無効な場合はnull
     */
    public static String validateToken(String token) {
        TokenData tokenData = tokenStorage.get(token);
        if (tokenData == null) {
            return null;
        }
        
        // 有効期限をチェック
        LocalDateTime expiry = tokenData.getCreatedAt().plusMinutes(TOKEN_VALIDITY_MINUTES);
        if (LocalDateTime.now().isAfter(expiry)) {
            tokenStorage.remove(token);
            return null;
        }
        
        return tokenData.getAction();
    }
    
    /**
     * トークンを無効化する（使用後に呼び出す）
     * @param token 無効化するトークン
     */
    public static void invalidateToken(String token) {
        tokenStorage.remove(token);
    }
    
    /**
     * 期限切れのトークンをクリーンアップする
     */
    private static void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        tokenStorage.entrySet().removeIf(entry -> {
            LocalDateTime expiry = entry.getValue().getCreatedAt().plusMinutes(TOKEN_VALIDITY_MINUTES);
            return now.isAfter(expiry);
        });
    }
    
    /**
     * QRコードを生成してBase64エンコードされた画像データとして返す
     * @param data QRコードに埋め込むデータ
     * @param width QRコードの幅
     * @param height QRコードの高さ
     * @return Base64エンコードされた画像データ
     * @throws WriterException QRコード生成時のエラー
     * @throws IOException 画像変換時のエラー
     */
    public static String generateQRCodeImage(String data, int width, int height) 
            throws WriterException, IOException {
        
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);
        
        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height, hints);
        
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        image.createGraphics();
        
        Graphics2D graphics = (Graphics2D) image.getGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(Color.BLACK);
        
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (bitMatrix.get(i, j)) {
                    graphics.fillRect(i, j, 1, 1);
                }
            }
        }
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        byte[] imageBytes = outputStream.toByteArray();
        
        return Base64.getEncoder().encodeToString(imageBytes);
    }
    
    /**
     * QRコード用のURLを生成する
     * @param baseUrl ベースURL
     * @param token トークン
     * @return QRコード用URL
     */
    public static String generateQRCodeUrl(String baseUrl, String token) {
        return baseUrl + "/qr?token=" + token;
    }
    
    /**
     * トークンデータを保持する内部クラス
     */
    private static class TokenData {
        private final String action;
        private final LocalDateTime createdAt;
        
        public TokenData(String action, LocalDateTime createdAt) {
            this.action = action;
            this.createdAt = createdAt;
        }
        
        public String getAction() {
            return action;
        }
        
        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
    }
}