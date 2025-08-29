package com.example.attendance.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.attendance.dao.AuthenticatorDAO;
import com.example.attendance.dto.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webauthn4j.converter.jackson.WebAuthnJSONModule;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.AttestationConveyancePreference;
import com.webauthn4j.data.AuthenticatorSelectionCriteria;
import com.webauthn4j.data.PublicKeyCredentialCreationOptions;
import com.webauthn4j.data.PublicKeyCredentialDescriptor;
import com.webauthn4j.data.PublicKeyCredentialParameters;
import com.webauthn4j.data.PublicKeyCredentialRpEntity;
import com.webauthn4j.data.PublicKeyCredentialType;
import com.webauthn4j.data.PublicKeyCredentialUserEntity;
import com.webauthn4j.data.UserVerificationRequirement;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.util.Base64UrlUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/passkey/register/start")
public class PasskeyRegistrationStartServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(PasskeyRegistrationStartServlet.class);

    private final ObjectConverter objectConverter = new ObjectConverter();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new WebAuthnJSONModule(objectConverter));
    private final AuthenticatorDAO authenticatorDAO = new AuthenticatorDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");
        if (user == null) {
            // 管理者など別キーでログインしている場合を考慮してフォールバックを試す
            user = (User) session.getAttribute("loggedInUser");
            if (user != null) {
                logger.info("passkey register start: using loggedInUser for session id={}", session.getId());
            }
        }

        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"error\":\"ログインしていません。先に通常ログインしてください。\"}");
            logger.info("passkey register start blocked: user not logged in, session id={}", session.getId());
            return;
        }

        String username = user.getUsername();
        byte[] userId = username.getBytes(StandardCharsets.UTF_8);

        List<PublicKeyCredentialDescriptor> existingAuthenticators = authenticatorDAO.getAuthenticatorsByUsername(username);
        List<PublicKeyCredentialDescriptor> excludeCredentials = existingAuthenticators.stream()
                .map(auth -> new PublicKeyCredentialDescriptor(
                        PublicKeyCredentialType.PUBLIC_KEY,
                        ((PublicKeyCredentialDescriptor) auth).getId(),
                        null))
                .collect(Collectors.toList());

        Challenge challenge = new DefaultChallenge();
        session.setAttribute("challenge", Base64UrlUtil.encodeToString(challenge.getValue()));
        session.setAttribute("userId", username);

        PublicKeyCredentialUserEntity userEntity = new PublicKeyCredentialUserEntity(
                userId,
                username,
                username);

        PublicKeyCredentialParameters pkcParams1 = new PublicKeyCredentialParameters(
                PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256);
        PublicKeyCredentialParameters pkcParams2 = new PublicKeyCredentialParameters(
                PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.RS256);

        PublicKeyCredentialCreationOptions options = new PublicKeyCredentialCreationOptions(
                new PublicKeyCredentialRpEntity("localhost", "kintai-app"),
                userEntity,
                challenge,
                Arrays.asList(pkcParams1, pkcParams2),
                30000L,
                excludeCredentials,
                new AuthenticatorSelectionCriteria(
                        null,
                        true, // requireResidentKey を true に設定
                        UserVerificationRequirement.PREFERRED),
                AttestationConveyancePreference.NONE,
                null);

        resp.setContentType("application/json");
        resp.getWriter().write(objectMapper.writeValueAsString(options));
    }
}
