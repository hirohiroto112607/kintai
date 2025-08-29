package com.example.attendance.dao;

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
                    // Do NOT attempt to deserialize attested_credential_data here; some stored bytes
                    // may be CBOR/opaque and cause parsing exceptions. Leave attestedCredentialData null.
                    AttestedCredentialData attestedCredentialData = null;
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
        // Robust dynamic save that copes with schema differences:
        // - Enumerate authenticators table columns
        // - Build INSERT including columns present in the DB
        // - Provide sensible defaults for NOT NULL columns that the app doesn't know about
        try (Connection conn = DatabaseUtil.getConnection()) {

            byte[] attestedCredentialDataBytes = null;
            if (authenticator.getAttestedCredentialData() != null) {
                attestedCredentialDataBytes = objectConverter.getJsonConverter().writeValueAsBytes(authenticator.getAttestedCredentialData());
            }

            byte[] credentialId = authenticator.getCredentialId();
            if (credentialId == null && authenticator.getAttestedCredentialData() != null) {
                credentialId = authenticator.getAttestedCredentialData().getCredentialId();
            }

            if (credentialId == null) {
                throw new SQLException("credentialId is required for saving authenticator (null detected)");
            }

            // Discover columns for authenticators table in public schema
            String colSql = "SELECT column_name, is_nullable, data_type, column_default FROM information_schema.columns WHERE table_name = 'authenticators' AND table_schema = 'public'";
            List<String> insertCols = new ArrayList<>();
            List<Object> values = new ArrayList<>();
            List<Integer> sqlTypes = new ArrayList<>();

            try (PreparedStatement colStmt = conn.prepareStatement(colSql);
                 ResultSet colsRs = colStmt.executeQuery()) {
                while (colsRs.next()) {
                    String col = colsRs.getString("column_name");
                    String isNullable = colsRs.getString("is_nullable"); // "YES"/"NO"
                    String dataType = colsRs.getString("data_type"); // e.g. "bytea", "boolean", "character varying", "bigint"
                    String colDefault = colsRs.getString("column_default");

                    // skip serial primary key (id) — let DB handle it
                    if ("id".equals(col)) {
                        continue;
                    }

                    // decide value and SQL type
                    Object value = null;
                    int sqlType = java.sql.Types.VARCHAR;

                    if ("user_id".equals(col)) {
                        value = userId;
                        sqlType = java.sql.Types.VARCHAR;
                    } else if ("credential_id".equals(col)) {
                        value = credentialId;
                        sqlType = java.sql.Types.BINARY;
                    } else if ("attested_credential_data".equals(col)) {
                        value = attestedCredentialDataBytes != null ? attestedCredentialDataBytes : new byte[0];
                        sqlType = java.sql.Types.BINARY;
                    } else if ("sign_count".equals(col)) {
                        value = authenticator.getSignCount();
                        sqlType = java.sql.Types.BIGINT;
                    } else {
                        // For unknown columns, if nullable OR has default, we can skip providing a value.
                        boolean nullable = "YES".equalsIgnoreCase(isNullable);
                        boolean hasDefault = colDefault != null && !colDefault.trim().isEmpty();
                        if (nullable || hasDefault) {
                            // skip including this column in INSERT to allow DB defaults / nulls
                            continue;
                        }
                        // Not nullable and no default -> provide a reasonable fallback based on data_type
                        if (dataType != null) {
                            String dt = dataType.toLowerCase();
                            if (dt.contains("boolean")) {
                                value = Boolean.FALSE;
                                sqlType = java.sql.Types.BOOLEAN;
                            } else if (dt.contains("bytea")) {
                                value = new byte[0];
                                sqlType = java.sql.Types.BINARY;
                            } else if (dt.contains("bigint")) {
                                value = 0L;
                                sqlType = java.sql.Types.BIGINT;
                            } else if (dt.contains("integer") || dt.contains("int")) {
                                value = 0;
                                sqlType = java.sql.Types.INTEGER;
                            } else if (dt.contains("timestamp")) {
                                value = new java.sql.Timestamp(System.currentTimeMillis());
                                sqlType = java.sql.Types.TIMESTAMP;
                            } else {
                                // text, varchar, other stringy types
                                value = "";
                                sqlType = java.sql.Types.VARCHAR;
                            }
                        } else {
                            // fallback to empty string
                            value = "";
                            sqlType = java.sql.Types.VARCHAR;
                        }
                    }

                    // include column/value in insert lists
                    insertCols.add(col);
                    values.add(value);
                    sqlTypes.add(sqlType);
                }
            }

            if (insertCols.isEmpty()) {
                throw new SQLException("No writable columns discovered for authenticators table");
            }

            // Build INSERT statement
            StringBuilder sbCols = new StringBuilder();
            StringBuilder sbPlaceholders = new StringBuilder();
            for (int i = 0; i < insertCols.size(); i++) {
                if (i > 0) {
                    sbCols.append(", ");
                    sbPlaceholders.append(", ");
                }
                sbCols.append(insertCols.get(i));
                sbPlaceholders.append("?");
            }
            String insertSql = "INSERT INTO authenticators (" + sbCols.toString() + ") VALUES (" + sbPlaceholders.toString() + ")";

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                for (int i = 0; i < values.size(); i++) {
                    Object v = values.get(i);
                    int idx = i + 1;
                    int t = sqlTypes.get(i);
                    if (v == null) {
                        insertStmt.setNull(idx, t);
                        continue;
                    }
                    if (t == java.sql.Types.BINARY) {
                        insertStmt.setBytes(idx, (byte[]) v);
                    } else if (t == java.sql.Types.BIGINT) {
                        insertStmt.setLong(idx, ((Number) v).longValue());
                    } else if (t == java.sql.Types.INTEGER) {
                        insertStmt.setInt(idx, ((Number) v).intValue());
                    } else if (t == java.sql.Types.BOOLEAN) {
                        insertStmt.setBoolean(idx, (Boolean) v);
                    } else if (t == java.sql.Types.TIMESTAMP) {
                        insertStmt.setTimestamp(idx, (java.sql.Timestamp) v);
                    } else {
                        insertStmt.setString(idx, v.toString());
                    }
                }
                insertStmt.executeUpdate();
            }

            try {
                System.out.println("DEBUG AuthenticatorDAO.save inserted credentialId base64url: " + com.webauthn4j.util.Base64UrlUtil.encodeToString(credentialId));
            } catch (Throwable ignore) {}

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            // JSON conversion or metadata read errors
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
     * This implementation is more flexible: it does case-insensitive match and falls back
     * to other encodings via separate helper methods.
     */
    public com.example.attendance.dto.Authenticator findByCredentialIdHex(String hex) {
        String sql = "SELECT credential_id, user_id, attested_credential_data, sign_count FROM authenticators WHERE lower(encode(credential_id,'hex')) = lower(?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, hex);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    byte[] credentialId = rs.getBytes("credential_id");
                    String userId = rs.getString("user_id");
                    byte[] attestedCredentialDataBytes = rs.getBytes("attested_credential_data");
                    // Skip deserialization of attested_credential_data to avoid throwing during lookup.
                    AttestedCredentialData attestedCredentialData = null;
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

    /**
     * Fallback lookup: search by base64 representation of credential_id (Postgres encode(...,'base64'))
     * Some environments or tools may have stored the value as base64; this method helps matching that.
     */
    public com.example.attendance.dto.Authenticator findByCredentialIdBase64(String base64) {
        String sql = "SELECT credential_id, user_id, attested_credential_data, sign_count FROM authenticators WHERE encode(credential_id,'base64') = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, base64);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    byte[] credentialId = rs.getBytes("credential_id");
                    String userId = rs.getString("user_id");
                    byte[] attestedCredentialDataBytes = rs.getBytes("attested_credential_data");
                    // Skip deserialization of attested_credential_data to avoid throwing during lookup.
                    AttestedCredentialData attestedCredentialData = null;
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
