<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <title>QRコード打刻</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/style.css">
    <style>
        .qr-container {
            text-align: center;
            margin: 30px 0;
            padding: 20px;
            border: 2px dashed #ddd;
            border-radius: 10px;
            background-color: #f9f9f9;
        }
        
        .qr-image {
            margin: 20px 0;
            max-width: 300px;
            border: 1px solid #ddd;
            border-radius: 5px;
        }
        
        .qr-actions {
            display: flex;
            gap: 15px;
            justify-content: center;
            flex-wrap: wrap;
            margin: 20px 0;
        }
        
        .token-input {
            display: flex;
            gap: 10px;
            align-items: center;
            justify-content: center;
            margin: 20px 0;
            flex-wrap: wrap;
        }
        
        .token-input input[type="text"] {
            width: 300px;
            max-width: 100%;
        }
        
        .expiry-info {
            font-size: 0.9em;
            color: #666;
            margin-top: 10px;
        }
        
        .loading {
            display: none;
            color: #007bff;
        }
        
        .qr-instructions {
            background-color: #e3f2fd;
            border: 1px solid #2196f3;
            padding: 15px;
            border-radius: 5px;
            margin: 20px 0;
        }
        
        .qr-instructions h3 {
            margin-top: 0;
            color: #1976d2;
        }
        
        .qr-instructions ul {
            margin: 10px 0;
            padding-left: 20px;
        }
        
        @media (max-width: 600px) {
            .qr-actions {
                flex-direction: column;
                align-items: center;
            }
            
            .token-input {
                flex-direction: column;
            }
            
            .token-input input[type="text"] {
                width: 100%;
            }
        }
    </style>
</head>
<body>
<div class="container">
    <h1>QRコード打刻</h1>
    <p>ようこそ, <c:out value="${user.username}"/>さん</p>

    <c:if test="${not empty successMessage}">
        <p class="success-message"><c:out value="${successMessage}"/></p>
    </c:if>
    
    <c:if test="${not empty errorMessage}">
        <p class="error-message"><c:out value="${errorMessage}"/></p>
    </c:if>

    <div class="qr-instructions">
        <h3>QRコード打刻の使用方法</h3>
        <ul>
            <li>「ユーザーIDQRコード生成」で自分のユーザーIDのQRコードを生成</li>
            <li>生成されたQRコードをスキャン</li>
            <li>現在の状況に応じて自動的に出勤・退勤が判定されます</li>
            <li>ユーザーIDQRコードに有効期限はありません</li>
        </ul>
    </div>

    <div class="qr-actions">
        <button onclick="generateQRCode('user_id')" class="button" style="background-color: #28a745;">ユーザーIDQRコード生成</button>
        <a href="<c:url value='/qr?view=scanner'/>" class="button" style="background-color: #17a2b8;">QRコードスキャナー</a>
    </div>

    <div class="qr-container" id="qrContainer" style="display: none;">
        <h3 id="qrTitle">QRコード</h3>
        <div class="loading" id="loading">QRコードを生成中...</div>
        <img id="qrImage" class="qr-image" alt="QRコード" style="display: none;">
        <div class="expiry-info" id="expiryInfo" style="display: none;">
            有効期限: <span id="expiryTime"></span>
        </div>
        <div style="margin-top: 15px;" id="tokenInfo" style="display: none;">
            <small>トークン: <span id="tokenValue" style="font-family: monospace; background: #f0f0f0; padding: 2px 4px; border-radius: 3px;"></span></small>
        </div>
    </div>

    <div style="margin: 30px 0; padding: 20px; background-color: #f8f9fa; border-radius: 5px;">
        <h3>手動トークン入力</h3>
        <p>QRコードをスキャンできない場合は、トークンを手動で入力してください。</p>
        <form action="<c:url value='/qr'/>" method="post">
            <div class="token-input">
                <input type="hidden" name="action" value="manual_token">
                <input type="text" name="token" placeholder="トークンを入力してください" required>
                <input type="submit" value="打刻実行" class="button">
            </div>
        </form>
    </div>

    <div class="button-group">
        <a href="<c:url value='/attendance'/>" class="button secondary">勤怠メニューに戻る</a>
        <a href="<c:url value='/logout'/>" class="button secondary">ログアウト</a>
    </div>
</div>

<script>
function generateQRCode(action) {
    const container = document.getElementById('qrContainer');
    const loading = document.getElementById('loading');
    const qrImage = document.getElementById('qrImage');
    const qrTitle = document.getElementById('qrTitle');
    const expiryInfo = document.getElementById('expiryInfo');
    const expiryTime = document.getElementById('expiryTime');
    const tokenInfo = document.getElementById('tokenInfo');
    const tokenValue = document.getElementById('tokenValue');
    
    // コンテナを表示
    container.style.display = 'block';
    loading.style.display = 'block';
    qrImage.style.display = 'none';
    expiryInfo.style.display = 'none';
    tokenInfo.style.display = 'none';
    
    // タイトルを設定
    qrTitle.textContent = 'ユーザーIDQRコード（自動判定）';
    
    // QRコードを生成
    fetch('${pageContext.request.contextPath}/qr?action=' + action)
        .then(response => response.json())
        .then(data => {
            loading.style.display = 'none';
            
            if (data.success) {
                qrImage.src = data.qrCodeImage;
                qrImage.style.display = 'block';
                
                expiryTime.textContent = 'このQRコードに有効期限はありません';
                expiryInfo.style.display = 'block';
                expiryTime.style.color = '#28a745';
                tokenValue.textContent = data.userId || data.token;
                document.querySelector('#tokenInfo span:first-child').textContent = 'ユーザーID:';
                tokenInfo.style.display = 'block';
            } else {
                alert('QRコードの生成に失敗しました: ' + (data.error || '不明なエラー'));
                container.style.display = 'none';
            }
        })
        .catch(error => {
            loading.style.display = 'none';
            alert('QRコードの生成に失敗しました: ' + error.message);
            container.style.display = 'none';
        });
}
</script>
</body>
</html>