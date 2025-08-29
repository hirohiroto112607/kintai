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
        
        .qr-instructions li {
            margin: 5px 0;
        }
        
        .qr-instructions strong {
            color: #1976d2;
        }
        
        #qrCanvas {
            max-width: 100%;
            height: auto;
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
    <!-- QR Code Generator Library -->
    <script src="https://cdnjs.cloudflare.com/ajax/libs/qrcodejs/1.0.0/qrcode.min.js" integrity="sha512-CNgIRecGo7nphbeZ04Sc13ka07paqdeTu0WR1IM4kNcpmBAUSHSQX0FslNhTDadL4O5SAGapGt4FodqL8My0mA==" crossorigin="anonymous" referrerpolicy="no-referrer"></script>  

</head>
<body>
<div class="container">
    <h1>QRコード打刻</h1>
    <p>ようこそ, <c:out value="${user.username}"/>さん</p>
<c:choose>
    <c:when test="${user.role == 'admin' or user.role == 'ADMIN'}">
        <jsp:include page="admin_nav.jsp" flush="true" />
    </c:when>
    <c:when test="${user.role == 'employee' or user.role == 'EMPLOYEE'}">
        <jsp:include page="employee_nav.jsp" flush="true" />
    </c:when>
    <c:otherwise>
        <!-- デフォルトは従業員ナビを表示 -->
        <jsp:include page="employee_nav.jsp" flush="true" />
    </c:otherwise>
</c:choose>
    <c:if test="${not empty successMessage}">
        <p class="success-message"><c:out value="${successMessage}"/></p>
    </c:if>
    
    <c:if test="${not empty errorMessage}">
        <p class="error-message"><c:out value="${errorMessage}"/></p>
    </c:if>

    <div class="qr-instructions">
        <h3>QRコード打刻の使用方法</h3>
        <ul>
            <li><strong>サーバー生成:</strong> 「ユーザーIDQRコード生成（サーバー）」でサーバーサイドで生成されたQRコードを取得</li>
            <li><strong>JavaScript生成:</strong> 「ユーザーIDQRコード生成（JavaScript）」でブラウザ上で即座にQRコードを生成</li>
            <li>生成されたQRコードをスキャンして打刻</li>
            <li>現在の状況に応じて自動的に出勤・退勤が判定されます</li>
            <li>ユーザーIDQRコードに有効期限はありません</li>
            <li><strong>JavaScript生成の利点:</strong> オフラインでも使用可能、カスタムテキストでQRコード生成可能</li>
        </ul>
    </div>

    <div class="qr-actions">
        <button onclick="generateQRCode('user_id')" class="button" style="background-color: #28a745;">ユーザーIDQRコード生成（サーバー）</button>
        <button onclick="generateQRCodeJS()" class="button" style="background-color: #6f42c1;">ユーザーIDQRコード生成（JavaScript）</button>
        <a href="<c:url value='/qr?view=scanner'/>" class="button" style="background-color: #17a2b8;">QRコードスキャナー</a>
    </div>

    <!-- カスタムQRコード生成エリア -->
    <div class="qr-container" id="customQrContainer" style="display: none;">
        <h3>カスタムQRコード生成（JavaScript）</h3>
        <div class="token-input">
            <input type="text" id="customText" placeholder="QRコードに含めるテキストを入力" value="<c:out value='${user.username}'/>"/>
            <button onclick="generateCustomQR()" class="button">QRコード生成</button>
        </div>
        <canvas id="qrCanvas" style="display: none; border: 1px solid #ddd; border-radius: 5px; margin: 20px 0;"></canvas>
        <div id="customQrInfo" style="display: none; margin-top: 15px;">
            <small>生成されたテキスト: <span id="generatedText" style="font-family: monospace; background: #f0f0f0; padding: 2px 4px; border-radius: 3px;"></span></small>
        </div>
        <div style="margin-top: 15px;">
            <button onclick="downloadQR()" class="button secondary" id="downloadBtn" style="display: none;">QRコードをダウンロード</button>
        </div>
    </div>

    <!-- サーバー生成QRコードエリア -->
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

    <div class="button-group">
        <a href="<c:url value='/attendance'/>" class="button secondary">勤怠メニューに戻る</a>
        <a href="<c:url value='/logout'/>" class="button secondary">ログアウト</a>
    </div>
</div>

<script>
// QRCodeライブラリが読み込まれているかチェックする関数
function isQRCodeLibraryLoaded() {
    return typeof QRCode !== 'undefined';
}

// ライブラリが読み込まれるまで待機する関数
function waitForQRCodeLibrary(callback, timeout = 5000) {
    const startTime = Date.now();
    
    function check() {
        if (isQRCodeLibraryLoaded()) {
            callback();
        } else if (Date.now() - startTime < timeout) {
            setTimeout(check, 100);
        } else {
            alert('QRコードライブラリの読み込みに失敗しました。ページを再読み込みしてください。');
        }
    }
    
    check();
}

// JavaScriptでQRコード生成する関数
function generateQRCodeJS() {
    if (!isQRCodeLibraryLoaded()) {
        alert('QRコードライブラリを読み込み中です。少し待ってからもう一度お試しください。');
        waitForQRCodeLibrary(generateQRCodeJS);
        return;
    }
    
    const customContainer = document.getElementById('customQrContainer');
    const serverContainer = document.getElementById('qrContainer');
    
    // 要素が存在するかチェック
    if (!customContainer) {
        console.error('customQrContainer要素が見つかりません');
        alert('QRコード生成エリアが見つかりません。ページを再読み込みしてください。');
        return;
    }
    
    // サーバー生成コンテナが存在する場合は非表示
    if (serverContainer) {
        serverContainer.style.display = 'none';
    }
    
    // カスタム生成コンテナを表示
    customContainer.style.display = 'block';
    
    // ユーザーIDがデフォルトで入力されている場合は自動生成
    const customText = document.getElementById('customText');
    if (customText.value.trim()) {
        generateCustomQR();
    }
}

// カスタムテキストからQRコードを生成
function generateCustomQR() {
    if (!isQRCodeLibraryLoaded()) {
        alert('QRコードライブラリが読み込まれていません。ページを再読み込みしてください。');
        return;
    }
    
    const customText = document.getElementById('customText').value.trim();
    const canvas = document.getElementById('qrCanvas');
    const generatedText = document.getElementById('generatedText');
    const customQrInfo = document.getElementById('customQrInfo');
    const downloadBtn = document.getElementById('downloadBtn');
    
    if (!customText) {
        alert('テキストを入力してください。');
        return;
    }
    
    try {
        // canvasを非表示にして親要素をクリア
        canvas.style.display = 'none';
        const qrContainer = canvas.parentElement;
        
        // 既存のQRCodeインスタンスを削除
        const existingQr = qrContainer.querySelector('.qrcode-container');
        if (existingQr) {
            existingQr.remove();
        }
        
        // QRコード生成用のコンテナを作成
        const qrDiv = document.createElement('div');
        qrDiv.className = 'qrcode-container';
        qrDiv.style.display = 'inline-block';
        qrDiv.style.margin = '20px 0';
        qrContainer.insertBefore(qrDiv, canvas);
        
        // QRコードを生成（qrcodejsライブラリの正しいAPI）
        const qr = new QRCode(qrDiv, {
            text: customText,
            width: 300,
            height: 300,
            colorDark: '#000000',
            colorLight: '#ffffff',
            correctLevel: QRCode.CorrectLevel.M
        });
        
        console.log('QRコード生成成功');
        generatedText.textContent = customText;
        customQrInfo.style.display = 'block';
        downloadBtn.style.display = 'inline-block';
        
        // QRコードが生成された後にcanvasを更新（ダウンロード用）
        setTimeout(() => {
            const qrImg = qrDiv.querySelector('img');
            if (qrImg) {
                const ctx = canvas.getContext('2d');
                canvas.width = 300;
                canvas.height = 300;
                
                qrImg.onload = function() {
                    ctx.drawImage(qrImg, 0, 0, 300, 300);
                };
                
                if (qrImg.complete) {
                    ctx.drawImage(qrImg, 0, 0, 300, 300);
                }
            }
        }, 100);
        
    } catch (error) {
        console.error('QRコード生成中にエラーが発生しました:', error);
        alert('QRコードの生成中にエラーが発生しました: ' + error.message);
    }
}

// QRコードをダウンロード
function downloadQR() {
    const canvas = document.getElementById('qrCanvas');
    const customText = document.getElementById('customText').value.trim();
    const qrContainer = canvas.parentElement.querySelector('.qrcode-container');
    
    if (!qrContainer || qrContainer.style.display === 'none') {
        alert('ダウンロードするQRコードがありません。');
        return;
    }
    
    try {
        // QRCodeライブラリで生成された画像を取得
        const qrImg = qrContainer.querySelector('img');
        if (qrImg) {
            // 画像を直接ダウンロード
            const link = document.createElement('a');
            link.download = 'qrcode_' + (customText.replace(/[^a-zA-Z0-9]/g, '_')) + '.png';
            link.href = qrImg.src;
            link.click();
        } else if (canvas.getContext) {
            // canvasからダウンロード（フォールバック）
            const link = document.createElement('a');
            link.download = 'qrcode_' + (customText.replace(/[^a-zA-Z0-9]/g, '_')) + '.png';
            link.href = canvas.toDataURL();
            link.click();
        } else {
            alert('QRコードの画像が見つかりません。');
        }
    } catch (error) {
        console.error('ダウンロードエラー:', error);
        alert('QRコードのダウンロードに失敗しました。');
    }
}

// サーバーサイドでQRコード生成する既存の関数
function generateQRCode(action) {
    const container = document.getElementById('qrContainer');
    const customContainer = document.getElementById('customQrContainer');
    const loading = document.getElementById('loading');
    const qrImage = document.getElementById('qrImage');
    const qrTitle = document.getElementById('qrTitle');
    const expiryInfo = document.getElementById('expiryInfo');
    const expiryTime = document.getElementById('expiryTime');
    const tokenInfo = document.getElementById('tokenInfo');
    const tokenValue = document.getElementById('tokenValue');
    
    // カスタム生成コンテナを非表示
    customContainer.style.display = 'none';
    
    // サーバー生成コンテナを表示
    container.style.display = 'block';
    loading.style.display = 'block';
    qrImage.style.display = 'none';
    expiryInfo.style.display = 'none';
    tokenInfo.style.display = 'none';
    
    // タイトルを設定
    qrTitle.textContent = 'ユーザーIDQRコード（サーバー生成・自動判定）';
    
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

// ページ読み込み時にQRCodeライブラリの状態をチェック
document.addEventListener('DOMContentLoaded', function() {
    console.log('ページ読み込み完了');
    
    // QRCodeライブラリの読み込み状態をチェック
    setTimeout(function() {
        if (isQRCodeLibraryLoaded()) {
            console.log('QRCodeライブラリが正常に読み込まれました');
        } else {
            console.warn('QRCodeライブラリの読み込みに時間がかかっています');
        }
    }, 1000);
});
</script>
</body>
</html>