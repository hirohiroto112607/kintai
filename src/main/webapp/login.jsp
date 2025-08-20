
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>勤怠管理システム - ログイン</title>
    <link rel="stylesheet"href="${pageContext.request.contextPath}/style.css">
</head>
<body>
    <div class="container">
        <h1>勤怠管理システム</h1>
        <form action="<c:url value='/login'/>" method="post">
            <p>
                <label for="username">ユーザーID:</label>
                <input type="text" id="username" name="username" required>
            </p>
            <p>
                <label for="password">パスワード:</label>
                <input type="password" id="password" name="password" required>
            </p>
            <div class="button-group">
                <input type="submit" value="ログイン" class="button">
            </div>
        </form>
        <c:if test="${not empty errorMessage}">
            <p class="error-message"><c:out value="${errorMessage}"/></p>
        </c:if>
    </div>
</body>
</html>
