package com.example.attendance.controller;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.example.attendance.dao.AuthenticatorDAO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webauthn4j.converter.jackson.WebAuthnJSONModule;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.PublicKeyCredentialDescriptor;
import com.webauthn4j.data.PublicKeyCredentialRequestOptions;
import com.webauthn4j.data.PublicKeyCredentialType;
import com.webauthn4j.data.UserVerificationRequirement;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.util.Base64UrlUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Passkey認証開始サーブレット
 * 一時的に無効化 - WebAuthn4J APIの変更のため
 * TODO: 正しいWebAuthn4J APIで再実装が必要
 */

 // @WebServlet("/passkey/auth/start")
@WebServlet("/passkey/authenticate/start")
public class PasskeyAuthenticationStartServlet extends HttpServlet {

    private final ObjectConverter objectConverter = new ObjectConverter();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new WebAuthnJSONModule(objectConverter));
    private final AuthenticatorDAO authenticatorDAO = new AuthenticatorDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = req.getParameter("username");
        Challenge challenge = new DefaultChallenge();

        req.getSession().setAttribute("challenge", Base64UrlUtil.encodeToString(challenge.getValue()));

        List<PublicKeyCredentialDescriptor> allowCredentials = null;
        // ユーザー名が指定されている場合は、そのユーザーのクレデンシャルに限定する（オプション）
        if (username != null && !username.isEmpty()) {
            List<PublicKeyCredentialDescriptor> authenticators = authenticatorDAO.getAuthenticatorsByUsername(username);
            allowCredentials = authenticators.stream()
                .map(auth -> new PublicKeyCredentialDescriptor(
                        PublicKeyCredentialType.PUBLIC_KEY,
                        auth.getId(),
                        null))
                .collect(Collectors.toList());
        }

        // allowCredentialsがnullまたは空の場合、クライアントはDiscoverable Credentialを検索する
        PublicKeyCredentialRequestOptions options = new PublicKeyCredentialRequestOptions(
                challenge,
                30000L, // timeout
                "localhost", // rpId
                allowCredentials,
                UserVerificationRequirement.PREFERRED,
                null);

        resp.setContentType("application/json");
        resp.getWriter().write(objectMapper.writeValueAsString(options));
    }
}
