<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>QRコードスキャナー</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/style.css">
    <style>
        .scanner-container {
            text-align: center;
            margin: 20px 0;
        }
        
        #qr-reader {
            width: 100%;
            max-width: 500px;
            margin: 0 auto;
            border: 2px solid #ddd;
            border-radius: 10px;
            overflow: hidden;
        }
        
        .scanner-controls {
            margin: 20px 0;
            display: flex;
            flex-direction: column;
            gap: 10px;
            align-items: center;
        }
        
        .camera-select {
            padding: 10px;
            border: 1px solid #ddd;
            border-radius: 5px;
            font-size: 16px;
            width: 100%;
            max-width: 300px;
        }
        
        .scan-result {
            margin: 20px 0;
            padding: 15px;
            border-radius: 5px;
            background-color: #f8f9fa;
        }
        
        .mobile-optimized {
            font-size: 18px;
            line-height: 1.6;
        }
        
        .status-message {
            padding: 10px;
            margin: 10px 0;
            border-radius: 5px;
            font-weight: bold;
        }
        
        .status-success {
            background-color: #d4edda;
            color: #155724;
            border: 1px solid #c3e6cb;
        }
        
        .status-error {
            background-color: #f8d7da;
            color: #721c24;
            border: 1px solid #f5c6cb;
        }
        
        .status-info {
            background-color: #d1ecf1;
            color: #0c5460;
            border: 1px solid #bee5eb;
        }
        
        @media (max-width: 600px) {
            .container {
                padding: 15px;
                margin: 10px;
            }
            
            h1 {
                font-size: 1.8em;
            }
            
            .button {
                padding: 15px 20px;
                font-size: 16px;
            }
        }
    </style>
    <!-- QR Scanner Library -->
    <script src="https://unpkg.com/html5-qrcode@2.3.8/html5-qrcode.min.js"></script>
</head>
<body>
<div class="container mobile-optimized">
    <h1>QRコードスキャナー</h1>
    
    <c:if test="${not empty user}">
        <p>ようこそ, <c:out value="${user.username}"/>さん</p>
    </c:if>

    <c:if test="${not empty successMessage}">
        <div class="status-message status-success">
            <c:out value="${successMessage}"/>
        </div>
    </c:if>
    
    <c:if test="${not empty errorMessage}">
        <div class="status-message status-error">
            <c:out value="${errorMessage}"/>
        </div>
    </c:if>

    <div class="scanner-container">
        <div id="status-message" class="status-message status-info" style="display: none;">
            スキャナーを起動しています...
        </div>
        
        <div class="scanner-controls">
            <select id="camera-select" class="camera-select">
                <option value="">カメラを選択してください</option>
            </select>
            <button id="start-scan" class="button" onclick="startScanning()">スキャン開始</button>
            <button id="stop-scan" class="button secondary" onclick="stopScanning()" style="display: none;">スキャン停止</button>
        </div>
        
        <div id="qr-reader"></div>
        
        <div id="scan-result" class="scan-result" style="display: none;">
            <h3>スキャン結果</h3>
            <p id="scan-text"></p>
            <button id="process-scan" class="button" onclick="processScanResult()">打刻実行</button>
        </div>
    </div>

    <div style="margin: 30px 0; padding: 20px; background-color: #f8f9fa; border-radius: 5px;">
        <h3>使用方法</h3>
        <ol>
            <li>カメラを選択して「スキャン開始」をクリック</li>
            <li>QRコードをカメラに向けてスキャン</li>
            <li>スキャン成功後、「打刻実行」をクリック</li>
            <li>打刻完了後、自動的にスキャンが継続されます</li>
        </ol>
        <p><strong>QRコード:</strong> ユーザーIDQRコード（有効期限なし、状況に応じて自動判定）</p>
        <p><strong>連続打刻:</strong> 一度スキャンを開始すれば、何度でも連続してQRコードをスキャン・打刻できます</p>
    </div>

    <div class="button-group">
        <a href="<c:url value='/qr'/>" class="button secondary">QRコード生成画面へ</a>
        <a href="<c:url value='/attendance'/>" class="button secondary">勤怠メニューに戻る</a>
    </div>
</div>

<script>
let html5QrcodeScanner;
let scannedUrl = null;
let lastScanned = null;
let lastScannedTime = 0;
let isSubmitting = false;

// ページ読み込み時にカメラリストを取得
document.addEventListener('DOMContentLoaded', function() {
    Html5Qrcode.getCameras().then(devices => {
        const cameraSelect = document.getElementById('camera-select');
        
        if (devices && devices.length) {
            devices.forEach((device, index) => {
                const option = document.createElement('option');
                option.value = device.id;
                option.text = device.label || `カメラ ${index + 1}`;
                cameraSelect.appendChild(option);
            });
            
            // 背面カメラを優先的に選択
            const backCamera = devices.find(device => 
                device.label.toLowerCase().includes('back') || 
                device.label.toLowerCase().includes('rear') ||
                device.label.toLowerCase().includes('environment')
            );
            if (backCamera) {
                cameraSelect.value = backCamera.id;
            } else if (devices.length > 0) {
                cameraSelect.value = devices[0].id;
            }
        } else {
            showStatus('カメラが見つかりませんでした。', 'error');
        }
    }).catch(err => {
        console.error('カメラの取得に失敗しました:', err);
        showStatus('カメラの取得に失敗しました。', 'error');
    });
});

function startScanning() {
    const cameraSelect = document.getElementById('camera-select');
    const selectedCamera = cameraSelect.value;
    
    if (!selectedCamera) {
        showStatus('カメラを選択してください。', 'error');
        return;
    }
    
    showStatus('スキャナーを起動中...', 'info');
    
    html5QrcodeScanner = new Html5Qrcode("qr-reader");
    
    html5QrcodeScanner.start(
        selectedCamera,
        {
            fps: 10,
            qrbox: { width: 250, height: 250 }
        },
        (decodedText, decodedResult) => {
            console.log('QRコードスキャン成功:', decodedText);
            onScanSuccess(decodedText);
        },
        (errorMessage) => {
            // スキャンエラーは頻繁に発生するので、コンソールのみにログ出力
            // console.log('スキャンエラー:', errorMessage);
        }
    ).then(() => {
        showStatus('スキャン中... QRコードをカメラに向けてください。', 'info');
        document.getElementById('start-scan').style.display = 'none';
        document.getElementById('stop-scan').style.display = 'inline-block';
        document.getElementById('camera-select').disabled = true;
    }).catch(err => {
        console.error('スキャナーの開始に失敗しました:', err);
        showStatus('スキャナーの開始に失敗しました: ' + err, 'error');
    });
}

function stopScanning() {
    if (html5QrcodeScanner) {
        html5QrcodeScanner.stop().then(() => {
            showStatus('スキャンを停止しました。', 'info');
        }).catch(err => {
            console.error('スキャナーの停止に失敗しました:', err);
        });
    }
    
    document.getElementById('start-scan').style.display = 'inline-block';
    document.getElementById('stop-scan').style.display = 'none';
    document.getElementById('camera-select').disabled = false;
    document.getElementById('scan-result').style.display = 'none';
    hideStatus();
}

function onScanSuccess(decodedText) {
    const now = Date.now();

    // 短時間内の重複スキャンを無視（3秒以内の同じコードは無視）
    if (decodedText === lastScanned && (now - lastScannedTime) < 3000) {
        return;
    }

    lastScanned = decodedText;
    lastScannedTime = now;
    scannedUrl = decodedText;

    // スキャンは継続したまま、結果だけ表示する
    document.getElementById('scan-text').textContent = decodedText;
    document.getElementById('scan-result').style.display = 'block';
    showStatus('QRコードを検出しました（スキャンは継続しています）', 'success');
}

function processScanResult() {
    if (isSubmitting) {
        return; // 二重送信防止
    }

    if (!scannedUrl) {
        showStatus('スキャン結果がありません。', 'error');
        return;
    }

    const userId = scannedUrl.trim();
    if (!userId || userId.length === 0) {
        showStatus('有効なユーザーIDが見つかりません。', 'error');
        return;
    }

    isSubmitting = true;
    const processBtn = document.getElementById('process-scan');
    processBtn.disabled = true;
    showStatus('打刻処理中...', 'info');

    // AJAXで打刻処理を実行
    const params = new URLSearchParams();
    params.append('action', 'user_id_scan');
    params.append('userId', userId);

    fetch('${pageContext.request.contextPath}/qr', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
        },
        body: params
    })
    .then(response => {
        if (!response.ok) {
            // サーバーからのエラーレスポンス（例: 404, 500）を処理
            // JSON形式のエラー詳細を期待する場合
            return response.json().then(err => {
                throw new Error(err.error || 'サーバーエラーが発生しました。');
            }).catch(() => {
                // JSONパースに失敗した場合のフォールバック
                throw new Error(`サーバーエラー: ${response.statusText} (${response.status})`);
            });
        }
        return response.json();
    })
    .then(data => {
        if (data.success) {
            showStatus(data.message, 'success');
            // 成功後2秒でスキャン結果をリセット
            setTimeout(() => {
                document.getElementById('scan-result').style.display = 'none';
                scannedUrl = null;
                showStatus('スキャン中... QRコードをカメラに向けてください。', 'info');
            }, 2000);
        } else {
            showStatus(data.error, 'error');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showStatus('通信エラーが発生しました: ' + error.message, 'error');
    })
    .finally(() => {
        isSubmitting = false;
        processBtn.disabled = false;
    });
}

function showStatus(message, type) {
    const statusElement = document.getElementById('status-message');
    statusElement.textContent = message;
    statusElement.className = 'status-message status-' + type;
    statusElement.style.display = 'block';
}

function hideStatus() {
    document.getElementById('status-message').style.display = 'none';
}
</script>
</body>
</html>