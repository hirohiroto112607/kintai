<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <title>部署管理</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/style.css">
</head>
<body>
<div class="container">
    <h1>部署管理</h1>
    <p>ようこそ, <c:out value="${user.username}"/>さん (管理者)</p>

    <div class="main-nav">
        <jsp:include page="admin_nav.jsp" flush="true" />
    </div>

    <c:if test="${not empty successMessage}">
        <p class="success-message"><c:out value="${successMessage}"/></p>
    </c:if>
    <c:if test="${not empty errorMessage}">
        <p class="error-message"><c:out value="${errorMessage}"/></p>
    </c:if>

    <h2>部署追加/編集</h2>
    <form action="<c:url value='/departments'/>" method="post" class="user-form">
        <input type="hidden" name="action" value="${not empty departmentToEdit ? 'update' : 'add'}">
        <c:if test="${not empty departmentToEdit}">
            <input type="hidden" name="departmentId" value="<c:out value='${departmentToEdit.departmentId}'/>">
        </c:if>
        
        <label for="departmentId">部署ID:</label>
        <input type="text" id="departmentId" name="departmentId" value="<c:out value='${departmentToEdit.departmentId}'/>" ${not empty departmentToEdit ? 'readonly' : ''} required>

        <label for="departmentName">部署名:</label>
        <input type="text" id="departmentName" name="departmentName" value="<c:out value='${departmentToEdit.departmentName}'/>" required>
        
        <label for="description">説明:</label>
        <textarea id="description" name="description" rows="3"><c:out value="${departmentToEdit.description}"/></textarea>
        
        <label for="enabled">状態:</label>
        <div>
            <input type="checkbox" id="enabled" name="enabled" ${(empty departmentToEdit or departmentToEdit.enabled) ? 'checked' : ''}> 有効
        </div>
        
        <div class="button-group" style="grid-column: 1 / 3;">
            <input type="submit" value="${not empty departmentToEdit ? '更新' : '追加'}" class="button">
        </div>
    </form>

    <h2>既存部署</h2>
    <table>
        <thead>
        <tr><th>部署ID</th><th>部署名</th><th>説明</th><th>状態</th><th>操作</th></tr>
        </thead>
        <tbody>
        <c:forEach var="dept" items="${departments}">
            <tr>
                <td><c:out value="${dept.departmentId}"/></td>
                <td><c:out value="${dept.departmentName}"/></td>
                <td><c:out value="${dept.description}"/></td>
                <td>${dept.enabled ? '有効' : '無効'}</td>
                <td class="table-actions">
                    <a href="<c:url value='/departments?action=edit&departmentId=${dept.departmentId}'/>" class="button">編集</a>
                    <form action="<c:url value='/departments'/>" method="post" style="display:inline;" onsubmit="return confirm('この部署を削除しますか？');">
                        <input type="hidden" name="action" value="delete">
                        <input type="hidden" name="departmentId" value="<c:out value='${dept.departmentId}'/>">
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