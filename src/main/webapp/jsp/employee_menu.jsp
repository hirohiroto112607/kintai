
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

    <div class="button-group">
        <form action="<c:url value='/attendance'/>" method="post" style="display:inline;">
            <input type="hidden" name="action" value="check_in">
            <input type="submit" value="出勤" class="button">
        </form>
        <form action="<c:url value='/attendance'/>" method="post" style="display:inline;">
            <input type="hidden" name="action" value="check_out">
            <input type="submit" value="退勤" class="button">
        </form>
        <a href="<c:url value='/qr'/>" class="button" style="background-color: #28a745;">QRコード打刻</a>
        <a href="<c:url value='/leave-requests'/>" class="button" style="background-color: #17a2b8;">休暇申請</a>
    </div>

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
</body>
</html>
