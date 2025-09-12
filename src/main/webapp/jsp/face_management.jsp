<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>顔データ管理 - 勤怠管理システム</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/style.css">
    <style>
        .face-data-list {
            margin: 20px 0;
        }
        .face-data-item {
            background: #f8f9fa;
            border: 1px solid #dee2e6;
            border-radius: 8px;
            padding: 15px;
            margin: 10px 0;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .face-data-info {
            flex: 1;
        }
        .face-data-actions {
            display: flex;
            gap: 10px;
        }
        .add-face-button {
            background: #28a745;
            color: white;
            padding: 10px 20px;
            border: none;
            border-radius: 5px;
            text-decoration: none;
            display: inline-block;
            margin: 10px 0;
        }
        .add-face-button:hover {
            background: #218838;
        }
        .face-index {
            font-weight: bold;
            color: #495057;
        }
        .face-label {
            color: #6c757d;
            font-size: 0.9em;
        }
        .face-dates {
            color: #6c757d;
            font-size: 0.8em;
        }
        .empty-state {
            text-align: center;
            padding: 40px;
            color: #6c757d;
        }
    </style>
</head>
<body>
<div class="container">
    <h1>顔データ管理</h1>
    <p>ユーザー: <strong><c:out value="${username}"/></strong></p>

    <c:if test="${not empty successMessage}">
        <div class="alert alert-success">
            <c:out value="${successMessage}"/>
        </div>
    </c:if>

    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger">
            <c:out value="${errorMessage}"/>
        </div>
    </c:if>

    <div class="actions">
        <a href="${pageContext.request.contextPath}/jsp/face_register.jsp" class="add-face-button">
            新しい顔データを登録
        </a>
        <a href="${pageContext.request.contextPath}/face/authenticate" class="button">
            顔認証テスト
        </a>
    </div>

    <div class="face-data-list">
        <h2>登録済み顔データ一覧</h2>
        
        <c:choose>
            <c:when test="${not empty faceDataList}">
                <c:forEach var="faceData" items="${faceDataList}">
                    <div class="face-data-item">
                        <div class="face-data-info">
                            <div class="face-index">顔データ #<c:out value="${faceData.faceIndex}"/></div>
                            <div class="face-label">ラベル: <c:out value="${faceData.label}"/></div>
                            <div class="face-dates">
                                登録日: <fmt:formatDate value="${faceData.createdAt}" pattern="yyyy/MM/dd HH:mm"/>
                                <c:if test="${faceData.updatedAt != faceData.createdAt}">
                                    | 更新日: <fmt:formatDate value="${faceData.updatedAt}" pattern="yyyy/MM/dd HH:mm"/>
                                </c:if>
                            </div>
                        </div>
                        <div class="face-data-actions">
                            <a href="${pageContext.request.contextPath}/jsp/face_register.jsp?edit=true&faceIndex=${faceData.faceIndex}" 
                               class="button">編集</a>
                            <form action="${pageContext.request.contextPath}/face/manage" method="post" 
                                  style="display:inline;" 
                                  onsubmit="return confirm('この顔データを削除しますか？');">
                                <input type="hidden" name="action" value="delete">
                                <input type="hidden" name="faceIndex" value="${faceData.faceIndex}">
                                <button type="submit" class="button danger">削除</button>
                            </form>
                        </div>
                    </div>
                </c:forEach>
            </c:when>
            <c:otherwise>
                <div class="empty-state">
                    <h3>顔データが登録されていません</h3>
                    <p>「新しい顔データを登録」ボタンから顔を登録してください。</p>
                </div>
            </c:otherwise>
        </c:choose>
    </div>

    <div class="navigation">
        <c:choose>
            <c:when test="${user.role == 'admin'}">
                <a href="${pageContext.request.contextPath}/attendance" class="button secondary">管理者メニューに戻る</a>
            </c:when>
            <c:otherwise>
                <a href="${pageContext.request.contextPath}/attendance" class="button secondary">従業員メニューに戻る</a>
            </c:otherwise>
        </c:choose>
    </div>
</div>
</body>
</html>