<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <title>休暇申請管理</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/style.css">
    <style>
        .table-actions form { display: inline; margin-right: 6px; }
        .rejection-reason { width: 220px; }
    </style>
    <script>
        function confirmReject(form){
            const reason = form.querySelector('input[name="rejectionReason"]').value.trim();
            if(!reason){
                alert('却下理由を入力してください');
                return false;
            }
            return confirm('この申請を却下します。よろしいですか？');
        }
    </script>
    </head>
<body>
<div class="container">
    <h1>休暇申請管理</h1>

    <div class="main-nav">
        <a href="<c:url value='/attendance'/>" class="button">管理者メニューに戻る</a>
        <a href="<c:url value='/logout'/>" class="button secondary">ログアウト</a>
    </div>

    <c:if test="${not empty successMessage}">
        <p class="success-message"><c:out value="${successMessage}"/></p>
    </c:if>
    <c:if test="${not empty errorMessage}">
        <p class="error-message"><c:out value="${errorMessage}"/></p>
    </c:if>

    <h2>承認待ち申請</h2>
    <table>
        <thead>
        <tr>
            <th>申請者</th>
            <th>申請日</th>
            <th>休暇種別</th>
            <th>期間</th>
            <th>日数</th>
            <th>理由</th>
            <th>操作</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="request" items="${pendingRequests}">
            <tr>
                <td><c:out value="${request.userId}"/></td>
                <td><fmt:formatDate value="${request.appliedAt}" pattern="yyyy-MM-dd"/></td>
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
                    <fmt:formatDate value="${request.startDate}" pattern="yyyy-MM-dd"/> 〜
                    <fmt:formatDate value="${request.endDate}" pattern="yyyy-MM-dd"/>
                </td>
                <td><c:out value="${request.daysCount}"/>日</td>
                <td><c:out value="${request.reason}"/></td>
                <td class="table-actions">
                    <form action="<c:url value='/leave-requests'/>" method="post">
                        <input type="hidden" name="action" value="approve">
                        <input type="hidden" name="requestId" value="${request.id}">
                        <input type="submit" value="承認" class="button">
                    </form>
                    <form action="<c:url value='/leave-requests'/>" method="post" onsubmit="return confirmReject(this);">
                        <input type="hidden" name="action" value="reject">
                        <input type="hidden" name="requestId" value="${request.id}">
                        <input type="text" name="rejectionReason" class="rejection-reason" placeholder="却下理由">
                        <input type="submit" value="却下" class="button danger">
                    </form>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>

    <h2>全申請履歴</h2>
    <table>
        <thead>
        <tr>
            <th>申請者</th>
            <th>申請日</th>
            <th>休暇種別</th>
            <th>期間</th>
            <th>状態</th>
            <th>承認者</th>
            <th>処理日</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="request" items="${allRequests}">
            <tr>
                <td><c:out value="${request.userId}"/></td>
                <td><fmt:formatDate value="${request.appliedAt}" pattern="yyyy-MM-dd"/></td>
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
                    <fmt:formatDate value="${request.startDate}" pattern="yyyy-MM-dd"/> 〜
                    <fmt:formatDate value="${request.endDate}" pattern="yyyy-MM-dd"/>
                </td>
                <td>
                    <c:choose>
                        <c:when test="${request.status == 'pending' || request.pending}">申請中</c:when>
                        <c:when test="${request.status == 'approved' || request.approved}">承認済み</c:when>
                        <c:when test="${request.status == 'rejected' || request.rejected}">却下</c:when>
                        <c:otherwise><c:out value="${request.status}"/></c:otherwise>
                    </c:choose>
                </td>
                <td><c:out value="${request.approverUserId}"/></td>
                <td>
                    <c:if test="${request.reviewedAt != null}">
                        <fmt:formatDate value="${request.reviewedAt}" pattern="yyyy-MM-dd"/>
                    </c:if>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>

    <c:if test="${not empty allRequests && empty pendingRequests}">
        <p>現在、承認待ちの申請はありません。</p>
    </c:if>
</div>
</body>
</html>
