package com.example.attendance.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.util.Date;

/**
 * JWTトークンの生成・検証を行うユーティリティクラス
 */
public class TokenUtil {

    // JWT秘密鍵（本番環境では環境変数や設定ファイルから読み込む）
    private static final String SECRET_KEY = "your-secret-key-here-change-in-production";

    // トークンの有効期限（1年）
    private static final long EXPIRATION_TIME = 365L * 24 * 60 * 60 * 1000; // 1年（ミリ秒）

    private static final Algorithm ALGORITHM = Algorithm.HMAC256(SECRET_KEY);

    /**
     * ユーザー情報からJWTトークンを生成
     * @param username ユーザー名
     * @param role ロール
     * @return JWTトークン
     */
    public static String generateToken(String username, String role) {
        Date issuedAt = new Date();
        Date expiresAt = new Date(issuedAt.getTime() + EXPIRATION_TIME);

        return JWT.create()
                .withSubject(username)
                .withClaim("role", role)
                .withIssuedAt(issuedAt)
                .withExpiresAt(expiresAt)
                .sign(ALGORITHM);
    }

    /**
     * JWTトークンを検証し、ユーザー情報を取得
     * @param token JWTトークン
     * @return 検証済みトークン情報
     * @throws JWTVerificationException トークンが無効な場合
     */
    public static DecodedJWT verifyToken(String token) throws JWTVerificationException {
        JWTVerifier verifier = JWT.require(ALGORITHM).build();
        return verifier.verify(token);
    }

    /**
     * トークンからユーザー名を取得
     * @param token JWTトークン
     * @return ユーザー名
     * @throws JWTVerificationException トークンが無効な場合
     */
    public static String getUsernameFromToken(String token) throws JWTVerificationException {
        DecodedJWT decodedJWT = verifyToken(token);
        return decodedJWT.getSubject();
    }

    /**
     * トークンからロールを取得
     * @param token JWTトークン
     * @return ロール
     * @throws JWTVerificationException トークンが無効な場合
     */
    public static String getRoleFromToken(String token) throws JWTVerificationException {
        DecodedJWT decodedJWT = verifyToken(token);
        return decodedJWT.getClaim("role").asString();
    }

    /**
     * トークンが有効期限内かチェック
     * @param token JWTトークン
     * @return 有効期限内ならtrue
     */
    public static boolean isTokenExpired(String token) {
        try {
            DecodedJWT decodedJWT = verifyToken(token);
            return decodedJWT.getExpiresAt().before(new Date());
        } catch (JWTVerificationException e) {
            return true; // 無効なトークンは期限切れ扱い
        }
    }
}
