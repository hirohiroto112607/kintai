
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <title>ユーザー管理</title>
    <link rel="stylesheet"href="${pageContext.request.contextPath}/style.css">
</head>
<body>
<div class="container">
    <h1>ユーザー管理</h1>
    <p>ようこそ, <c:out value="${user.username}"/>さん (管理者)</p>

    <div class="main-nav">
        <a href="<c:url value='/attendance'/>" class="button">勤怠履歴管理</a>
        <a href="<c:url value='/users'/>" class="button">ユーザー管理</a>
        <a href="<c:url value='/qr'/>" class="button" style="background-color: #28a745;">QRコード打刻</a>
        <a href="<c:url value='/logout'/>" class="button secondary">ログアウト</a>
    </div>

    <c:if test="${not empty successMessage}">
        <p class="success-message"><c:out value="${successMessage}"/></p>
    </c:if>
    <c:if test="${not empty errorMessage}">
        <p class="error-message"><c:out value="${errorMessage}"/></p>
    </c:if>

    <h2>ユーザー追加/編集</h2>
    <form action="<c:url value='/users'/>" method="post" class="user-form">
        <input type="hidden" name="action" value="${not empty userToEdit ? 'update' : 'add'}">
        <c:if test="${not empty userToEdit}">
            <input type="hidden" name="username" value="<c:out value="${userToEdit.username}"/>">
        </c:if>
        
        <label for="username">ユーザーID:</label>
        <input type="text" id="username" name="username" value="<c:out value="${userToEdit.username}"/>" ${not empty userToEdit ? 'readonly' : ''} required>

        <label for="password">パスワード:</label>
        <input type="password" id="password" name="password" ${empty userToEdit ? 'required' : ''}>
        
        <label for="role">役割:</label>
        <select id="role" name="role" required>
            <option value="employee" ${userToEdit.role == 'employee' ? 'selected' : ''}>従業員</option>
            <option value="admin" ${userToEdit.role == 'admin' ? 'selected' : ''}>管理者</option>
        </select>

        <label for="enabled">アカウント:</label>
        <div>
            <input type="checkbox" id="enabled" name="enabled" value="true" ${(empty userToEdit or userToEdit.enabled) ? 'checked' : ''}> 有効
        </div>

        <div class="button-group" style="grid-column: 1 / 3;">
            <input type="submit" value="${not empty userToEdit ? '更新' : '追加'}" class="button">
        </div>
    </form>

    <h2>既存ユーザー</h2>
    <table>
        <thead>
        <tr><th>ユーザーID</th><th>役割</th><th>状態</th><th>操作</th></tr>
        </thead>
        <tbody>
        <c:forEach var="u" items="${users}">
            <tr>
                <td><c:out value="${u.username}"/></td>
                <td><c:out value="${u.role}"/></td>
                <td>${u.enabled ? '有効' : '無効'}</td>
                <td class="table-actions">
                    <a href="<c:url value='/users?action=edit&username=${u.username}'/>" class="button">編集</a>
                    <form action="<c:url value='/users'/>" method="post" style="display:inline;" onsubmit="return confirm('パスワードをリセットしますか？');">
                        <input type="hidden" name="action" value="reset_password">
                        <input type="hidden" name="username" value="<c:out value="${u.username}"/>">
                        <input type="submit" value="PWリセット" class="button secondary">
                    </form>
                    <form action="<c:url value='/users'/>" method="post" style="display:inline;" onsubmit="return confirm('このユーザーを削除しますか？');">
                        <input type="hidden" name="action" value="delete">
                        <input type="hidden" name="username" value="<c:out value="${u.username}"/>">
                        <input type="submit" value="削除" class="button danger">
                    </form>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</div>
</body>
</html>
