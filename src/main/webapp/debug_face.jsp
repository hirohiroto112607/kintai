<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.example.attendance.dao.FaceDataDAO" %>
<%@ page import="java.sql.SQLException" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <title>顔認証デバッグページ</title>
</head>
<body>
    <h1>顔認証デバッグ情報</h1>
    
    <%
        try {
            FaceDataDAO faceDataDAO = new FaceDataDAO();
            String faceDescriptorsJson = faceDataDAO.getAllFaceDescriptors();
            
            out.println("<h2>結果:</h2>");
            out.println("<p>データ取得成功</p>");
            out.println("<p>データ長: " + (faceDescriptorsJson != null ? faceDescriptorsJson.length() : "null") + " 文字</p>");
            out.println("<h3>データ内容:</h3>");
            out.println("<pre style='background: #f0f0f0; padding: 10px; overflow: auto;'>");
            out.println(faceDescriptorsJson != null ? faceDescriptorsJson : "null");
            out.println("</pre>");
            
        } catch (SQLException e) {
            out.println("<h2>SQLエラー:</h2>");
            out.println("<p style='color: red;'>" + e.getMessage() + "</p>");
            out.println("<pre style='background: #ffe0e0; padding: 10px;'>");
            e.printStackTrace(new java.io.PrintWriter(out));
            out.println("</pre>");
        } catch (Exception e) {
            out.println("<h2>一般エラー:</h2>");
            out.println("<p style='color: red;'>" + e.getMessage() + "</p>");
            out.println("<pre style='background: #ffe0e0; padding: 10px;'>");
            e.printStackTrace(new java.io.PrintWriter(out));
            out.println("</pre>");
        }
    %>
    
    <p><a href="${pageContext.request.contextPath}/login.jsp">ログインページに戻る</a></p>
</body>
</html>