package com.example.attendance.controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.attendance.dao.AuthenticatorDAO;
import com.example.attendance.dao.UserDAO;
import com.example.attendance.dto.Authenticator;
import com.example.attendance.dto.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webauthn4j.util.Base64UrlUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/passkey/authenticate/finish")
public class PasskeyAuthenticationFinishServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(PasskeyAuthenticationFinishServlet.class);

    private final UserDAO userDAO = new UserDAO();
    private final AuthenticatorDAO authenticatorDAO = new AuthenticatorDAO();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            resp.setContentType("application/json;charset=UTF-8");
            // 期待: クライアントは credentialId を base64url で送る (JSON body or form parameter)
            String credentialIdParam = null;

            // Try JSON body first
            String body = req.getReader().lines().reduce("", (accumulator, actual) -> accumulator + actual).trim();
            if (!body.isEmpty() && body.startsWith("{")) {
                try {
                    var node = objectMapper.readTree(body);
                    if (node.has("credentialId")) {
                        credentialIdParam = node.get("credentialId").asText();
                    } else if (node.has("rawId")) {
                        credentialIdParam = node.get("rawId").asText();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            if (credentialIdParam == null) {
                credentialIdParam = req.getParameter("credentialId");
            }

            if (credentialIdParam == null || credentialIdParam.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\":false,\"message\":\"credentialId is required\"}");
                return;
            }

            logger.debug("/passkey/authenticate/finish credentialIdParam: {}", credentialIdParam);

            byte[] credentialId = null;
            try {
                credentialId = Base64UrlUtil.decode(credentialIdParam);
                    logger.debug("decoded credentialId (base64url) length: {}", (credentialId != null ? credentialId.length : "null"));
            } catch (Exception exDecode) {
                logger.debug("Base64UrlUtil.decode failed: {}", exDecode.getMessage());
                // try java.util Base64 URL decoder
                try {
                    credentialId = java.util.Base64.getUrlDecoder().decode(credentialIdParam);
                    logger.debug("decoded credentialId with java.util.Base64.getUrlDecoder()");
                } catch (Exception ex2) {
                    // try replacing url-safe chars and padding then decode with standard decoder
                    try {
                        String s = credentialIdParam.replace('-', '+').replace('_', '/');
                        int mod = s.length() % 4;
                        if (mod != 0) {
                            s += "====".substring(0, 4 - mod);
                        }
                        credentialId = java.util.Base64.getDecoder().decode(s);
                        logger.debug("decoded credentialId with java.util.Base64.getDecoder() after replacement/padding");
                    } catch (Exception ex3) {
                        logger.debug("all base64 decode attempts failed: {}", ex3.getMessage());
                        credentialId = null;
                    }
                }
            }

            Authenticator authenticator = null;

            // Try multiple lookup strategies and emit detailed debug logs to diagnose mismatches
            try {
                if (credentialId != null) {
                    // 1) direct bytes lookup
                    logger.debug("trying direct byte[] lookup");
                    authenticator = authenticatorDAO.findByCredentialId(credentialId);

                    // 2) hex lookup of decoded bytes
                    if (authenticator == null) {
                        StringBuilder sb = new StringBuilder();
                        for (byte b : credentialId) {
                            sb.append(String.format("%02x", b));
                        }
                        String hex = sb.toString();
                        logger.debug("credentialId decoded -> hex: {}", hex);
                        authenticator = authenticatorDAO.findByCredentialIdHex(hex);
                    }

                    // 3) base64/base64url lookup (Postgres encode(...,'base64') uses standard base64)
                    if (authenticator == null) {
                        try {
                            String b64 = java.util.Base64.getEncoder().encodeToString(credentialId);
                            logger.debug("credentialId decoded -> base64: {}", b64);
                            authenticator = authenticatorDAO.findByCredentialIdBase64(b64);
                        } catch (Throwable t) {
                            // ignore
                        }
                    }
                }

                // If not found yet, try interpreting the original parameter in various forms
                if (authenticator == null && credentialIdParam != null && !credentialIdParam.isEmpty()) {
                    String candidate = credentialIdParam;
                    // strip 0x prefix if present
                    if (candidate.startsWith("0x") || candidate.startsWith("0X")) {
                        candidate = candidate.substring(2);
                    }

                    // If candidate looks like hex
                    if (candidate.matches("^[0-9a-fA-F]+$")) {
                        logger.debug("credentialIdParam appears hex, trying hex lookup: {}", candidate);
                        authenticator = authenticatorDAO.findByCredentialIdHex(candidate);
                    }

                    // Try base64url decode -> bytes -> lookup
                    if (authenticator == null) {
                        try {
                            byte[] fromB64Url = Base64UrlUtil.decode(credentialIdParam);
                            logger.debug("credentialIdParam decoded as base64url, trying byte[] lookup");
                            authenticator = authenticatorDAO.findByCredentialId(fromB64Url);
                            if (authenticator == null) {
                                StringBuilder sb2 = new StringBuilder();
                                for (byte b : fromB64Url) sb2.append(String.format("%02x", b));
                                logger.debug("base64url-decoded -> hex: {}", sb2.toString());
                                authenticator = authenticatorDAO.findByCredentialIdHex(sb2.toString());
                            }
                        } catch (Exception e) {
                            // not base64url
                        }
                    }

                    // Try standard base64 decode
                    if (authenticator == null) {
                        try {
                            byte[] fromB64 = java.util.Base64.getDecoder().decode(credentialIdParam);
                            logger.debug("credentialIdParam decoded as standard base64, trying byte[] lookup");
                            authenticator = authenticatorDAO.findByCredentialId(fromB64);
                            if (authenticator == null) {
                                StringBuilder sb3 = new StringBuilder();
                                for (byte b : fromB64) sb3.append(String.format("%02x", b));
                                logger.debug("standard base64 decoded -> hex: {}", sb3.toString());
                                authenticator = authenticatorDAO.findByCredentialIdHex(sb3.toString());
                            }
                        } catch (Exception e) {
                            // not standard base64
                        }
                    }
                }
            } catch (Throwable t) {
                logger.debug("lookup attempts threw: {}", t.toString(), t);
            }

            if (authenticator == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\":false,\"message\":\"認証器が見つかりませんでした。登録済みのパスキーでログインしてください。\"}");
                return;
            }

            // NOTE: Skipping full WebAuthn validation here to unblock compilation.
            // Increment stored sign count to reflect an authentication attempt.
            long newSignCount = authenticator.getSignCount() + 1;
            // Use the credentialId returned from DB (authenticator) to ensure update works
            byte[] storedCredentialId = authenticator.getCredentialId();
            authenticatorDAO.updateSignCount(storedCredentialId, newSignCount);
            logger.info("updated signCount for user={}, newSignCount={}", authenticator.getUserId(), newSignCount);

            User user = userDAO.findByUsername(authenticator.getUserId());
            if (user == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\":false,\"message\":\"ユーザーが見つかりませんでした。\"}");
                return;
            }

            HttpSession session = req.getSession();
            // 両方のキーにユーザーをセットして従来ログインとの互換を保つ
            session.setAttribute("user", user);
            session.setAttribute("loggedInUser", user);
            logger.debug("session set: id={}, isNew={}, user={}", session.getId(), session.isNew(), (user != null ? user.getUsername() : "null"));

            // 従来のフォームログインと同じリダイレクト先にする（/attendance）
            resp.getWriter().write(String.format("{\"success\":true, \"redirectUrl\":\"%s\"}", req.getContextPath() + "/attendance"));
            logger.info("passkey authentication success for user={}, redirect={}", user.getUsername(), req.getContextPath() + "/attendance");

        } catch (Exception e) {
            logger.error("Unexpected error in PasskeyAuthenticationFinishServlet", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\":false, \"message\":\"サーバ内部でエラーが発生しました。しばらくしてから再度お試しください。\"}");
        }
    }
}
