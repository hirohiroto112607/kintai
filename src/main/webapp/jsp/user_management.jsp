
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
        <a href="<c:url value='/departments'/>" class="button">部署管理</a>
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
        
        <label for="departmentId">部署:</label>
        <select id="departmentId" name="departmentId">
            <option value="">未所属</option>
            <c:forEach var="dept" items="${departments}">
                <option value="<c:out value="${dept.departmentId}"/>" ${userToEdit.departmentId == dept.departmentId ? 'selected' : ''}>
                    <c:out value="${dept.departmentName}"/>
                </option>
            </c:forEach>
        </select>
        
        <label for="enabled">アカウント:</label>
        <div>
            <input type="checkbox" id="enabled" name="enabled" value="true" ${(empty userToEdit or userToEdit.enabled) ? 'checked' : ''}> 有効
        </div>        <div class="button-group" style="grid-column: 1 / 3;">
            <input type="submit" value="${not empty userToEdit ? '更新' : '追加'}" class="button">
        </div>
    </form>

    <h2>既存ユーザー</h2>
    <table>
        <thead>
        <tr><th>ユーザーID</th><th>役割</th><th>部署</th><th>状態</th><th>操作</th></tr>
        </thead>
        <tbody>
        <c:forEach var="u" items="${users}">
            <tr>
                <td><c:out value="${u.username}"/></td>
                <td><c:out value="${u.role}"/></td>
                <td>
                    <c:choose>
                        <c:when test="${not empty u.departmentId}">
                            <c:forEach var="dept" items="${departments}">
                                <c:if test="${dept.departmentId == u.departmentId}">
                                    <c:out value="${dept.departmentName}"/>
                                </c:if>
                            </c:forEach>
                        </c:when>
                        <c:otherwise>未所属</c:otherwise>
                    </c:choose>
                </td>
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

<!-- 
休暇申請システムのJSPファイル例：

1. 従業員向け休暇申請ページ (leave_requests.jsp):
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <title>休暇申請</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/style.css">
</head>
<body>
<div class="container">
    <h1>休暇申請</h1>
    
    <div class="main-nav">
        <a href="<c:url value='/attendance'/>" class="button">勤怠メニューに戻る</a>
    </div>
    
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
        <textarea id="reason" name="reason" rows="3" required></textarea>
        
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
                <td><fmt:formatDate value="${request.appliedAt}" pattern="yyyy-MM-dd"/></td>
                <td>
                    <c:choose>
                        <c:when test="${request.leaveType == 'annual'}">年次有給休暇</c:when>
                        <c:when test="${request.leaveType == 'sick'}">病気休暇</c:when>
                        <c:when test="${request.leaveType == 'personal'}">私用休暇</c:when>
                        <c:when test="${request.leaveType == 'maternity'}">産前産後休暇</c:when>
                        <c:when test="${request.leaveType == 'paternity'}">育児休暇</c:when>
                        <c:otherwise>${request.leaveType}</c:otherwise>
                    </c:choose>
                </td>
                <td><fmt:formatDate value="${request.startDate}" pattern="yyyy-MM-dd"/> 〜 <fmt:formatDate value="${request.endDate}" pattern="yyyy-MM-dd"/></td>
                <td>${request.daysCount}日</td>
                <td>
                    <c:choose>
                        <c:when test="${request.pending}">申請中</c:when>
                        <c:when test="${request.approved}">承認済み</c:when>
                        <c:when test="${request.rejected}">却下</c:when>
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

2. 管理者向け休暇申請管理ページ (leave_management.jsp):
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <title>休暇申請管理</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/style.css">
</head>
<body>
<div class="container">
    <h1>休暇申請管理</h1>
    
    <div class="main-nav">
        <a href="<c:url value='/attendance'/>" class="button">管理者メニューに戻る</a>
    </div>
    
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
                        <c:otherwise>${request.leaveType}</c:otherwise>
                    </c:choose>
                </td>
                <td><fmt:formatDate value="${request.startDate}" pattern="yyyy-MM-dd"/> 〜 <fmt:formatDate value="${request.endDate}" pattern="yyyy-MM-dd"/></td>
                <td>${request.daysCount}日</td>
                <td><c:out value="${request.reason}"/></td>
                <td class="table-actions">
                    <form action="<c:url value='/leave-requests'/>" method="post" style="display:inline;">
                        <input type="hidden" name="action" value="approve">
                        <input type="hidden" name="requestId" value="${request.id}">
                        <input type="submit" value="承認" class="button">
                    </form>
                    <form action="<c:url value='/leave-requests'/>" method="post" style="display:inline;" onsubmit="return prompt('却下理由を入力してください');">
                        <input type="hidden" name="action" value="reject">
                        <input type="hidden" name="requestId" value="${request.id}">
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
                        <c:otherwise>${request.leaveType}</c:otherwise>
                    </c:choose>
                </td>
                <td><fmt:formatDate value="${request.startDate}" pattern="yyyy-MM-dd"/> 〜 <fmt:formatDate value="${request.endDate}" pattern="yyyy-MM-dd"/></td>
                <td>
                    <c:choose>
                        <c:when test="${request.pending}">申請中</c:when>
                        <c:when test="${request.approved}">承認済み</c:when>
                        <c:when test="${request.rejected}">却下</c:when>
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
</div>
</body>
</html>
-->

</body>
</html>
