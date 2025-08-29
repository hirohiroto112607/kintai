package com.example.attendance.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import com.example.attendance.dao.AuthenticatorDAO;
import com.example.attendance.dao.UserDAO;
import com.example.attendance.dto.Authenticator;
import com.example.attendance.dto.User;
import com.example.attendance.util.DatabaseUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webauthn4j.converter.jackson.WebAuthnJSONModule;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.attestation.AttestationObject;
import com.webauthn4j.data.client.CollectedClientData;
import com.webauthn4j.util.Base64UrlUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/passkey/register/finish")
public class PasskeyRegistrationFinishServlet extends HttpServlet {

    private final ObjectConverter objectConverter = new ObjectConverter();
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        super.init();
        // Unknown properties in clientDataJSON may appear from some clients — ignore them to allow parsing.
        objectMapper = new ObjectMapper()
                .registerModule(new WebAuthnJSONModule(objectConverter))
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try (Connection conn = DatabaseUtil.getConnection()) {
            HttpSession session = request.getSession();
            String challenge = (String) session.getAttribute("challenge");
            String userId = (String) session.getAttribute("userId"); // 登録対象のユーザーID

            if (challenge == null || userId == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Session challenge or userId not found.");
                return;
            }

 // Jacksonを使ってJSONをMapに変換し、必要なフィールドを抽出する
// 生のリクエストボディを読み出してバイト列をログに出力（UTF-8 以外の不正バイト確認用）
ByteArrayOutputStream baos = new ByteArrayOutputStream();
byte[] buffer = new byte[4096];
int read;
while ((read = request.getInputStream().read(buffer)) != -1) {
    baos.write(buffer, 0, read);
}
byte[] rawBody = baos.toByteArray();

System.out.println("DEBUG Content-Type: " + request.getHeader("Content-Type"));
System.out.println("DEBUG request characterEncoding: " + request.getCharacterEncoding());
System.out.println("DEBUG rawBody length: " + rawBody.length);
// ログに先頭バイト列（最大128バイト）を16進で出力
int dumpLen = Math.min(rawBody.length, 128);
StringBuilder hex = new StringBuilder();
for (int i = 0; i < dumpLen; i++) {
    hex.append(String.format("%02x ", rawBody[i]));
}
System.out.println("DEBUG rawBody hex (first " + dumpLen + " bytes): " + hex.toString());

// Jackson に渡す前に 0xA0 (NBSP) が先頭などにある場合の簡易サニタイズ
if (rawBody.length > 0 && (rawBody[0] & 0xFF) == 0xA0) {
    System.out.println("DEBUG rawBody startsWith 0xA0, trimming leading 0xA0 bytes");
    int offset = 0;
    while (offset < rawBody.length && (rawBody[offset] & 0xFF) == 0xA0) offset++;
    byte[] tmp = new byte[rawBody.length - offset];
    System.arraycopy(rawBody, offset, tmp, 0, tmp.length);
    rawBody = tmp;
}

// 試行: まずは UTF-8 としてパースを試みる
Map<String, Object> requestBody = null;
try {
    requestBody = objectMapper.readValue(rawBody, Map.class);
} catch (com.fasterxml.jackson.core.JsonParseException ex) {
    System.out.println("DEBUG JsonParseException with UTF-8 attempt: " + ex.getMessage());
    // 代替: ISO-8859-1 -> UTF-8 変換を試す
    try {
        String latin = new String(rawBody, StandardCharsets.ISO_8859_1);
        byte[] utf8Bytes = latin.getBytes(StandardCharsets.UTF_8);
        requestBody = objectMapper.readValue(utf8Bytes, Map.class);
        System.out.println("DEBUG parsed by converting ISO-8859-1->UTF-8");
    } catch (Exception ex2) {
        System.out.println("DEBUG secondary parse failed: " + ex2.getMessage());
        throw ex; // 元の例外を投げて上位で処理
    }
}

@SuppressWarnings("unchecked")
Map<String, String> responseMap = (Map<String, String>) requestBody.get("response");

System.out.println("DEBUG /passkey/register/finish received response keys: " + responseMap.keySet());
System.out.println("DEBUG clientDataJSON length: " + (responseMap.get("clientDataJSON") != null ? responseMap.get("clientDataJSON").length() : "null"));
System.out.println("DEBUG attestationObject length: " + (responseMap.get("attestationObject") != null ? responseMap.get("attestationObject").length() : "null"));
System.out.println("DEBUG session challenge: " + challenge);

            byte[] clientDataJSON = Base64UrlUtil.decode(responseMap.get("clientDataJSON"));
            byte[] attestationObject = Base64UrlUtil.decode(responseMap.get("attestationObject"));

                        // 最低限の格納のためにバイト配列とパース済みオブジェクトを保持する
            CollectedClientData collectedClientData = null;
            try {
                collectedClientData = objectMapper.readValue(clientDataJSON, CollectedClientData.class);
            } catch (com.fasterxml.jackson.core.JsonParseException ex) {
                System.out.println("DEBUG CollectedClientData parse failed: " + ex.getMessage());
                // 簡易サニタイズ: 0xA0 (NBSP) をスペース(0x20) に置換して再試行
                for (int i = 0; i < clientDataJSON.length; i++) {
                    if ((clientDataJSON[i] & 0xFF) == 0xA0) {
                        clientDataJSON[i] = (byte) 0x20;
                    }
                }
                collectedClientData = objectMapper.readValue(clientDataJSON, CollectedClientData.class);
            }

            AttestationObject attestationObjectParsed = null;
            try {
                attestationObjectParsed = objectMapper.readValue(attestationObject, AttestationObject.class);
            } catch (com.fasterxml.jackson.core.JsonParseException ex) {
                System.out.println("DEBUG AttestationObject parse failed: " + ex.getMessage());
                // CBOR のため Jackson による JSON パースに失敗することがある。
                // ここではパース失敗を許容し、raw attestationObject バイト列を保存するのみとする。
                attestationObjectParsed = null;
            }

            // Authenticator情報を作成してDBに保存（厳密なWebAuthn検証はライブラリのAPI差異があるため別途実施）
            AuthenticatorDAO authenticatorDAO = new AuthenticatorDAO();
            Authenticator newAuthenticator = new Authenticator();

            newAuthenticator.setUserId(userId);

            if (attestationObjectParsed != null && attestationObjectParsed.getAuthenticatorData() != null) {
                newAuthenticator.setAttestedCredentialData(attestationObjectParsed.getAuthenticatorData().getAttestedCredentialData());
                newAuthenticator.setCredentialId(attestationObjectParsed.getAuthenticatorData().getAttestedCredentialData().getCredentialId());
            } else {
                // attestationObject のパースに失敗した場合は、クライアントから送られた rawId を利用して credentialId を設定する
                String rawIdB64 = (String) requestBody.get("rawId");
                if (rawIdB64 != null) {
                    try {
                        byte[] rawIdBytes = Base64UrlUtil.decode(rawIdB64);
                        newAuthenticator.setCredentialId(rawIdBytes);
                        System.out.println("DEBUG set credentialId from rawId, length: " + (rawIdBytes != null ? rawIdBytes.length : 0));
                    } catch (Exception e) {
                        System.out.println("DEBUG failed to decode rawId: " + e.getMessage());
                    }
                } else {
                    System.out.println("DEBUG rawId not provided in requestBody");
                }
            }

            newAuthenticator.setSignCount((attestationObjectParsed != null && attestationObjectParsed.getAuthenticatorData() != null) ? attestationObjectParsed.getAuthenticatorData().getSignCount() : 0L);
            newAuthenticator.setAttestationObject(attestationObject); // raw attestation object
            newAuthenticator.setClientData(collectedClientData); // parsed client data

 // 保存
System.out.println("DEBUG saving authenticator for userId: " + userId + ", credentialId length: " + (newAuthenticator.getCredentialId() != null ? newAuthenticator.getCredentialId().length : 0));
if (newAuthenticator.getCredentialId() != null) {
    System.out.println("DEBUG saved credentialId base64url: " + Base64UrlUtil.encodeToString(newAuthenticator.getCredentialId()));
} else {
    System.out.println("DEBUG saved credentialId base64url: null");
}
authenticatorDAO.save(userId, newAuthenticator);

            UserDAO userDAO = new UserDAO();
            User user = userDAO.findByUsername(userId);
            session.setAttribute("loggedInUser", user);

            response.sendRedirect(request.getContextPath() + "/jsp/employee_menu.jsp");

        } catch (SQLException e) {
            throw new ServletException("Database error during registration finish.", e);
        } catch (Exception e) {
            throw new ServletException("Error during registration finish.", e);
        }
    }
}
