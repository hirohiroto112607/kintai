<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <title>休暇申請</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/style.css">
    <style>
        .user-form { max-width: 720px; }
    </style>
</head>
<body>
<div class="container">
    <h1>休暇申請</h1>

    <div class="main-nav">
        <a href="<c:url value='/attendance'/>" class="button">勤怠メニューに戻る</a>
        <a href="<c:url value='/logout'/>" class="button secondary">ログアウト</a>
    </div>

    <c:if test="${not empty successMessage}">
        <p class="success-message"><c:out value="${successMessage}"/></p>
    </c:if>
    <c:if test="${not empty errorMessage}">
        <p class="error-message"><c:out value="${errorMessage}"/></p>
    </c:if>

    <h2>新規申請</h2>
    <form action="<c:url value='/leave-requests'/>" method="post" class="user-form">
        <input type="hidden" name="action" value="apply">

        <label for="leaveType">休暇種別:</label>
        <select id="leaveType" name="leaveType" required>
            <option value="annual">年次有給休暇</option>
            <option value="sick">病気休暇</option>
            <option value="personal">私用休暇</option>
            <option value="maternity">産前産後休暇</option>
            <option value="paternity">育児休暇</option>
        </select>

        <label for="startDate">開始日:</label>
        <input type="date" id="startDate" name="startDate" required>

        <label for="endDate">終了日:</label>
        <input type="date" id="endDate" name="endDate" required>

        <label for="reason">理由:</label>
        <textarea id="reason" name="reason" rows="3"></textarea>

        <div class="button-group">
            <input type="submit" value="申請" class="button">
        </div>
    </form>

    <h2>申請履歴</h2>
    <table>
        <thead>
        <tr>
            <th>申請日</th>
            <th>休暇種別</th>
            <th>期間</th>
            <th>日数</th>
            <th>状態</th>
            <th>承認者</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="request" items="${leaveRequests}">
            <tr>
                <td>${request.appliedAt.toString().substring(0, 10)}</td>
                <td>
                    <c:choose>
                        <c:when test="${request.leaveType == 'annual'}">年次有給休暇</c:when>
                        <c:when test="${request.leaveType == 'sick'}">病気休暇</c:when>
                        <c:when test="${request.leaveType == 'personal'}">私用休暇</c:when>
                        <c:when test="${request.leaveType == 'maternity'}">産前産後休暇</c:when>
                        <c:when test="${request.leaveType == 'paternity'}">育児休暇</c:when>
                        <c:otherwise><c:out value="${request.leaveType}"/></c:otherwise>
                    </c:choose>
                </td>
                <td>
                    ${request.startDate} 〜 ${request.endDate}
                </td>
                <td><c:out value="${request.daysCount}"/>日</td>
                <td>
                    <c:choose>
                        <c:when test="${request.status == 'pending' || request.pending}">申請中</c:when>
                        <c:when test="${request.status == 'approved' || request.approved}">承認済み</c:when>
                        <c:when test="${request.status == 'rejected' || request.rejected}">却下</c:when>
                        <c:otherwise><c:out value="${request.status}"/></c:otherwise>
                    </c:choose>
                </td>
                <td><c:out value="${request.approverUserId}"/></td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</div>
</body>
</html>
