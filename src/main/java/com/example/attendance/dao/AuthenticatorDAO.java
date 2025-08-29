package com.example.attendance.dao;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.example.attendance.dto.User;
import com.example.attendance.util.DatabaseUtil;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.PublicKeyCredentialDescriptor;
import com.webauthn4j.data.PublicKeyCredentialType;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;

public class AuthenticatorDAO {

    private final ObjectConverter objectConverter = new ObjectConverter();

    public List<PublicKeyCredentialDescriptor> getAuthenticatorsByUsername(String username) {
        List<PublicKeyCredentialDescriptor> authenticators = new ArrayList<>();
        String sql = "SELECT credential_id FROM authenticators WHERE user_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    authenticators.add(new PublicKeyCredentialDescriptor(
                            PublicKeyCredentialType.PUBLIC_KEY,
                            rs.getBytes("credential_id"),
                            null
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return authenticators;
    }

    public com.example.attendance.dto.Authenticator findByCredentialId(byte[] credentialId) {
        // DEBUG: incoming credentialId (base64url) for lookup
        try {
            if (credentialId != null) {
                System.out.println("DEBUG findByCredentialId lookup credentialId base64url: " + com.webauthn4j.util.Base64UrlUtil.encodeToString(credentialId));
            } else {
                System.out.println("DEBUG findByCredentialId lookup credentialId is null");
            }
        } catch (Throwable ignore) {}
        String sql = "SELECT user_id, attested_credential_data, sign_count FROM authenticators WHERE credential_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBytes(1, credentialId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String userId = rs.getString("user_id");
                    byte[] attestedCredentialDataBytes = rs.getBytes("attested_credential_data");
                    AttestedCredentialData attestedCredentialData = objectConverter.getJsonConverter().readValue(new ByteArrayInputStream(attestedCredentialDataBytes), AttestedCredentialData.class);
                    long signCount = rs.getLong("sign_count");
                    com.example.attendance.dto.Authenticator authenticator = new com.example.attendance.dto.Authenticator();
                    authenticator.setUserId(userId);
                    authenticator.setCredentialId(credentialId);
                    authenticator.setAttestedCredentialData(attestedCredentialData);
                    authenticator.setSignCount(signCount);
                    return authenticator;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void save(String userId, com.example.attendance.dto.Authenticator authenticator) {
        // public_key カラムが NOT NULL の環境があるため、public_key も必ず指定して挿入する。
        // uv_initialized 等の NOT NULL カラムが存在する DB に対応するため、必要なカラムを明示して挿入する
        String sql = "INSERT INTO authenticators (user_id, credential_id, public_key, attested_credential_data, uv_initialized, sign_count) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // attestedCredentialData を安全にシリアライズ（null 対応）
            byte[] attestedCredentialDataBytes = null;
            if (authenticator.getAttestedCredentialData() != null) {
                attestedCredentialDataBytes = objectConverter.getJsonConverter().writeValueAsBytes(authenticator.getAttestedCredentialData());
            }

            // credentialId は DTO の credentialId を優先し、なければ attestedCredentialData から取得する
            byte[] credentialId = authenticator.getCredentialId();
            if (credentialId == null && authenticator.getAttestedCredentialData() != null) {
                credentialId = authenticator.getAttestedCredentialData().getCredentialId();
            }

            // publicKeyBytes: 可能なら attestedCredentialData から取得、それ以外は attestedCredentialDataBytes を代用、
            // 最終的に null ではなく空配列を保存して NOT NULL 制約を満たす
            byte[] publicKeyBytes = null;
            try {
                if (authenticator.getAttestedCredentialData() != null) {
                    // AttestedCredentialData が持つ public key を取り出す（ライブラリの API に依存）
                    // safe access via reflection: avoid compile-time dependency on method that may not exist
                    try {
                        Object acd = authenticator.getAttestedCredentialData();
                        java.lang.reflect.Method m = acd.getClass().getMethod("getCredentialPublicKey");
                        Object pk = m.invoke(acd);
                        if (pk instanceof byte[]) {
                            publicKeyBytes = (byte[]) pk;
                        } else if (pk != null) {
                            // 最終手段で JSON シリアライズを使う
                            publicKeyBytes = objectConverter.getJsonConverter().writeValueAsBytes(pk);
                        }
                    } catch (NoSuchMethodException nsme) {
                        // method not present in this webauthn4j version - ignore and fallback
                    } catch (Throwable ignore) {
                        // other reflection errors - fallback
                    }
                }
            } catch (Throwable ignore) {}

            if (publicKeyBytes == null) {
                // フォールバック: attestedCredentialDataBytes があればそれを public_key に保存（簡易）
                if (attestedCredentialDataBytes != null) {
                    publicKeyBytes = attestedCredentialDataBytes;
                } else {
                    publicKeyBytes = new byte[0];
                }
            }

            pstmt.setString(1, userId);

            if (credentialId != null) {
                pstmt.setBytes(2, credentialId);
            } else {
                pstmt.setNull(2, java.sql.Types.BINARY);
            }

            // public_key は NOT NULL の可能性があるため、空配列ではなく NULL を避ける
            if (publicKeyBytes != null) {
                pstmt.setBytes(3, publicKeyBytes);
            } else {
                pstmt.setBytes(3, new byte[0]);
            }

            if (attestedCredentialDataBytes != null) {
                pstmt.setBytes(4, attestedCredentialDataBytes);
            } else {
                pstmt.setNull(4, java.sql.Types.BINARY);
            }

            // uv_initialized が NOT NULL の場合に備えて値を設定（デフォルト false）
            pstmt.setBoolean(5, authenticator.isUvInitialized());

            pstmt.setLong(6, authenticator.getSignCount());
            pstmt.executeUpdate();
            try {
                if (credentialId != null) {
                    System.out.println("DEBUG AuthenticatorDAO.save inserted credentialId base64url: " + com.webauthn4j.util.Base64UrlUtil.encodeToString(credentialId));
                } else {
                    System.out.println("DEBUG AuthenticatorDAO.save inserted credentialId is null");
                }
            } catch (Throwable ignore) {}
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void update(com.example.attendance.dto.Authenticator authenticator) {
        String sql = "UPDATE authenticators SET sign_count = ? WHERE credential_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, authenticator.getSignCount());
            pstmt.setBytes(2, authenticator.getCredentialId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateSignCount(byte[] credentialId, long newSignCount) {
        String sql = "UPDATE authenticators SET sign_count = ? WHERE credential_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, newSignCount);
            pstmt.setBytes(2, credentialId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public User findUserByCredentialId(byte[] credentialId) {
        String userId = null;
        String sql = "SELECT user_id FROM authenticators WHERE credential_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBytes(1, credentialId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    userId = rs.getString("user_id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (userId != null) {
            return new UserDAO().findByUsername(userId);
        }
        return null;
    }

    /**
     * Fallback lookup: search by hex representation of credential_id (Postgres encode(...,'hex'))
     * This helps recover from encoding/storage mismatches during debugging.
     */
    public com.example.attendance.dto.Authenticator findByCredentialIdHex(String hex) {
        String sql = "SELECT credential_id, user_id, attested_credential_data, sign_count FROM authenticators WHERE encode(credential_id,'hex') = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, hex);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    byte[] credentialId = rs.getBytes("credential_id");
                    String userId = rs.getString("user_id");
                    byte[] attestedCredentialDataBytes = rs.getBytes("attested_credential_data");
                    AttestedCredentialData attestedCredentialData = null;
                    if (attestedCredentialDataBytes != null) {
                        attestedCredentialData = objectConverter.getJsonConverter().readValue(new ByteArrayInputStream(attestedCredentialDataBytes), AttestedCredentialData.class);
                    }
                    long signCount = rs.getLong("sign_count");
                    com.example.attendance.dto.Authenticator authenticator = new com.example.attendance.dto.Authenticator();
                    authenticator.setUserId(userId);
                    authenticator.setCredentialId(credentialId);
                    authenticator.setAttestedCredentialData(attestedCredentialData);
                    authenticator.setSignCount(signCount);
                    return authenticator;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
