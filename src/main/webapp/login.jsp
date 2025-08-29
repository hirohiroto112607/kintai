<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ログイン - 勤怠管理システム</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <div class="container">
        <h1>勤怠管理システム</h1>
        
        <c:if test="${not empty param.error}">
            <p class="error-message">
                <c:choose>
                    <c:when test="${param.error == 'invalid_credentials'}">
                        ユーザー名またはパスワードが正しくありません。
                    </c:when>
                    <c:when test="${param.error == 'passkey_failed'}">
                        パスキー認証に失敗しました: ${fn:escapeXml(param.errorMessage)}
                    </c:when>
                    <c:otherwise>
                        ログイン中にエラーが発生しました。
                    </c:otherwise>
                </c:choose>
            </p>
        </c:if>

        <div class="login-form">
            <h2>ログイン</h2>
            <form action="login" method="post">
                <div class="form-group">
                    <label for="username">ユーザー名:</label>
                    <input type="text" id="username" name="username" autocomplete="username" required>
                </div>
                <div class="form-group">
                    <label for="password">パスワード:</label>
                    <input type="password" id="password" name="password" autocomplete="current-password" required>
                </div>
                <button type="submit">ログイン</button>
            </form>
        </div>

        <div class="passkey-login">
            <h2>パスキーでログイン</h2>
            <button id="passkey-login-button" type="button">パスキー認証</button>
            <p id="passkey-message" class="message"></p>
        </div>
    </div>

    <script>
        const passkeyLoginButton = document.getElementById('passkey-login-button');
        const passkeyMessage = document.getElementById('passkey-message');

        // Base64URLエンコード/デコードヘルパー
        const base64url = {
            encode: (buffer) => {
                return btoa(String.fromCharCode.apply(null, new Uint8Array(buffer)))
                    .replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
            },
            decode: (str) => {
                str = str.replace(/-/g, '+').replace(/_/g, '/');
                while (str.length % 4) {
                    str += '=';
                }
                const base64 = atob(str);
                const buffer = new Uint8Array(base64.length);
                for (let i = 0; i < base64.length; i++) {
                    buffer[i] = base64.charCodeAt(i);
                }
                return buffer;
            }
        };

        passkeyLoginButton.addEventListener('click', async () => {
            try {
                // 1. 認証チャレンジを開始 (ユーザー名なし)
                const startResponse = await fetch('<%= request.getContextPath() %>/passkey/authenticate/start');
                if (!startResponse.ok) {
                    let errorText = '認証チャレンジの開始に失敗しました。';
                    try {
                        const errorData = await startResponse.json();
                        errorText = errorData.message || errorText;
                    } catch (e) {
                        // ignore if response is not json
                    }
                    throw new Error(errorText);
                }
                const publicKeyCredentialRequestOptions = await startResponse.json();

                // Build minimal publicKey object to avoid sending unexpected fields (avoid 'hints' / invalid sequences)
                const publicKey = {
                    challenge: base64url.decode(publicKeyCredentialRequestOptions.challenge)
                };
                if (typeof publicKeyCredentialRequestOptions.timeout === 'number') {
                    publicKey.timeout = publicKeyCredentialRequestOptions.timeout;
                }
                if (typeof publicKeyCredentialRequestOptions.rpId === 'string') {
                    publicKey.rpId = publicKeyCredentialRequestOptions.rpId;
                }
                if (typeof publicKeyCredentialRequestOptions.userVerification === 'string') {
                    publicKey.userVerification = publicKeyCredentialRequestOptions.userVerification;
                }
                if (Array.isArray(publicKeyCredentialRequestOptions.allowCredentials)) {
                    publicKey.allowCredentials = publicKeyCredentialRequestOptions.allowCredentials.map(cred => {
                        const entry = {
                            type: cred.type || 'public-key',
                            id: base64url.decode(cred.id)
                        };
                        if (Array.isArray(cred.transports)) {
                            entry.transports = cred.transports;
                        }
                        return entry;
                    });
                }

                // 2. ブラウザのWebAuthn APIを呼び出す
                const assertion = await navigator.credentials.get({ publicKey });

                // 3. 認証結果をサーバーに送信
                const finishResponse = await fetch('<%= request.getContextPath() %>/passkey/authenticate/finish', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        credentialId: base64url.encode(assertion.rawId),
                        clientDataJSON: base64url.encode(assertion.response.clientDataJSON),
                        authenticatorData: base64url.encode(assertion.response.authenticatorData),
                        signature: base64url.encode(assertion.response.signature),
                        userHandle: assertion.response.userHandle ? base64url.encode(assertion.response.userHandle) : null
                    })
                });

                const finishData = await finishResponse.json();

                if (finishData.success) {
                    passkeyMessage.textContent = 'パスキー認証に成功しました。リダイレクトします...';
                    passkeyMessage.className = 'success-message';
                    window.location.href = finishData.redirectUrl;
                } else {
                    throw new Error(finishData.message || 'パスキー認証の完了に失敗しました。');
                }

            } catch (error) {
                console.error('パスキー認証エラー:', error);
                passkeyMessage.textContent = `エラー: ${error.message}`;
                passkeyMessage.className = 'error-message';
            }
        });
    </script>
</body>
</html>
