
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <title>エラー</title>
    <link rel="stylesheet"href="${pageContext.request.contextPath}/style.css">
</head>
<body>
    <div class="container">
        <h1>エラーが発生しました</h1>
        <p>申し訳ありませんが、処理中に予期せぬエラーが発生しました。</p>
        <%-- 開発環境では以下のコメントを解除すると詳細なエラーメッセージが確認できます --%>
        <%-- 
        <h2>エラー詳細</h2>
        <p><strong>メッセージ:</strong> <%= exception.getMessage() %></p>
        <pre>
        <% exception.printStackTrace(new java.io.PrintWriter(out)); %>
        </pre>
        --%>
        <a href="<c:url value='/login.jsp'/>">ログインページに戻る</a>
    </div>
</body>
</html>
