
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <title>従業員メニュー</title>
    <link rel="stylesheet"href="${pageContext.request.contextPath}/style.css">
</head>
<body>
<div class="container">
    <h1>従業員メニュー</h1>
    <p>ようこそ, <c:out value="${user.username}"/>さん</p>

    <c:if test="${not empty successMessage}">
        <p class="success-message"><c:out value="${successMessage}"/></p>
    </c:if>

    <jsp:include page="employee_nav.jsp" flush="true" />

    <h2>あなたの勤怠履歴</h2>
    <table>
        <thead>
        <tr>
            <th>出勤時刻</th>
            <th>退勤時刻</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="att" items="${attendanceRecords}">
            <tr>
                <td>
                    <c:choose>
                        <c:when test="${att.checkInTime != null}">
                            ${att.checkInTime.toString().replace('T', ' ')}
                        </c:when>
                        <c:otherwise>-</c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <c:choose>
                        <c:when test="${att.checkOutTime != null}">
                            ${att.checkOutTime.toString().replace('T', ' ')}
                        </c:when>
                        <c:otherwise>-</c:otherwise>
                    </c:choose>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty attendanceRecords}">
            <tr><td colspan="2">勤怠記録がありません。</td></tr>
        </c:if>
        </tbody>
    </table>

    <div class="button-group">
        <a href="<c:url value='/logout'/>" class="button secondary">ログアウト</a>
    </div>
</div>

<script>
    document.getElementById('registerPasskeyBtn').addEventListener('click', async () => {
        try {
            // 1. 登録オプションをサーバーから取得
            const response = await fetch('<c:url value="/passkey/register/start"/>');
            if (!response.ok) {
                throw new Error('パスキー登録の開始に失敗しました。');
            }
            const options = await response.json();

            // Base64UrlをArrayBufferに変換
            options.challenge = bufferDecode(options.challenge);
            options.user.id = bufferDecode(options.user.id);

            // 2. パスキーを作成
            const credential = await navigator.credentials.create({ publicKey: options });

            // 3. 公開鍵をサーバーに送信して保存
            const verificationResponse = await fetch('<c:url value="/passkey/register/finish"/>', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: new URLSearchParams({
                    clientDataJSON: bufferEncode(credential.response.clientDataJSON),
                    attestationObject: bufferEncode(credential.response.attestationObject),
                })
            });

            if (verificationResponse.ok) {
                alert('パスキーが正常に登録されました。');
            } else {
                const error = await verificationResponse.text();
                throw new Error(`パスキーの登録に失敗しました: ${error}`);
            }

        } catch (err) {
            console.error(err);
            alert(err.message);
        }
    });

    // Base64URL to ArrayBuffer
    function bufferDecode(value) {
        const s = atob(value.replace(/_/g, '/').replace(/-/g, '+'));
        const a = new Uint8Array(s.length);
        for (let i = 0; i < s.length; i++) {
            a[i] = s.charCodeAt(i);
        }
        return a;
    }

    // ArrayBuffer to Base64URL
    function bufferEncode(value) {
        return btoa(String.fromCharCode.apply(null, new Uint8Array(value)))
            .replace(/\+/g, '-')
            .replace(/\//g, '_')
            .replace(/=/g, '');
    }
</body>
</html>
