<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    String faceDescriptorsJson = (String) request.getAttribute("faceDescriptorsJson");
    if (faceDescriptorsJson == null) {
        faceDescriptorsJson = "[]";
    }
%>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>顔認証・QRコード勤怠打刻 - 勤怠管理システム</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/style.css">
    <script src="https://cdn.jsdelivr.net/npm/@tensorflow/tfjs@1.7.4/dist/tf.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/face-api.js@0.22.2/dist/face-api.min.js"></script>
    <script src="https://unpkg.com/html5-qrcode@2.3.8/html5-qrcode.min.js"></script>
    <script src="${pageContext.request.contextPath}/js/face-api-init.js"></script>
    <style>
        .attendance-container {
            max-width: 1000px;
            margin: 0 auto;
            padding: 20px;
        }
        .camera-section {
            display: flex;
            gap: 20px;
            margin: 20px 0;
        }
        .camera-container {
            flex: 1;
            position: relative;
        }
        #video {
            width: 100%;
            max-width: 640px;
            height: auto;
            border: 2px solid #ddd;
            border-radius: 8px;
        }
        .results-section {
            flex: 1;
            display: flex;
            flex-direction: column;
            gap: 20px;
        }
        .result-card {
            border: 1px solid #ddd;
            border-radius: 8px;
            padding: 15px;
            background-color: #f8f9fa;
        }
        .result-card h3 {
            margin-top: 0;
            color: #333;
        }
        .status-message {
            padding: 10px;
            margin: 10px 0;
            border-radius: 4px;
            text-align: center;
            font-weight: bold;
        }
        .status-success {
            background-color: #d4edda;
            color: #155724;
            border: 1px solid #c3e6cb;
        }
        .status-warning {
            background-color: #fff3cd;
            color: #856404;
            border: 1px solid #ffeaa7;
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
        .controls {
            text-align: center;
            margin: 20px 0;
        }
        .button {
            padding: 10px 20px;
            margin: 5px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 16px;
        }
        .button-primary {
            background-color: #007bff;
            color: white;
        }
        .button-secondary {
            background-color: #6c757d;
            color: white;
        }
        .debug-info {
            background: #f8f9fa;
            padding: 10px;
            margin: 10px 0;
            border-radius: 4px;
            font-family: monospace;
            font-size: 12px;
            max-height: 200px;
            overflow-y: auto;
            display: none;
        }
        @media (max-width: 768px) {
            .camera-section {
                flex-direction: column;
            }
        }
    </style>
</head>
<body>
    <div class="attendance-container">
        <h1>顔認証・QRコード勤怠打刻</h1>
        <p>カメラに顔を向けたり、QRコードをスキャンして勤怠を記録してください。</p>

        <div id="global-status" class="status-message status-info">
            システムを初期化しています...
        </div>

        <div class="camera-section">
            <div class="camera-container">
                <video id="video" autoplay muted playsinline></video>
                <div id="face-overlay" style="position: absolute; top: 0; left: 0; pointer-events: none;"></div>
            </div>

            <div class="results-section">
                <div class="result-card">
                    <h3>顔認証結果</h3>
                    <div id="face-status" class="status-message status-info">
                        顔認証待機中...
                    </div>
                    <div id="face-details"></div>
                </div>

                <div class="result-card">
                    <h3>QRコード結果</h3>
                    <div id="qr-status" class="status-message status-info">
                        QRコード待機中...
                    </div>
                    <div id="qr-details">
                        <div id="qr-detected" style="display: none;">
                            <p id="qr-text"></p>
                            <button id="qr-checkin-btn" class="button button-primary" onclick="processQRCheckin()">打刻実行</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="controls">
            <button id="toggle-debug" class="button button-secondary" onclick="toggleDebug()">デバッグ情報表示</button>
            <a href="${pageContext.request.contextPath}/attendance" class="button button-secondary">勤怠メニューに戻る</a>
        </div>

        <div id="debug-info" class="debug-info">
            <h4>デバッグ情報:</h4>
            <div id="debug-content"></div>
        </div>
    </div>

    <script>
        // グローバル変数
        const video = document.getElementById('video');
        const globalStatus = document.getElementById('global-status');
        const faceStatus = document.getElementById('face-status');
        const qrStatus = document.getElementById('qr-status');
        const debugContent = document.getElementById('debug-content');
        const faceDetails = document.getElementById('face-details');
        const qrDetails = document.getElementById('qr-details');

        const labeledFaceDescriptorsJson = JSON.parse('<%= faceDescriptorsJson %>');
        let faceMatcher;
        let html5QrcodeScanner;
        let faceDetectionInterval;
        let qrDetectionInterval;
        let isInitialized = false;

        // 顔認証用変数
        let lastDetectedUser = null;
        let detectionCount = 0;
        let consecutiveMatches = 0;
        let lastMatchedUser = null;
        const REQUIRED_CONSECUTIVE_MATCHES = 3;

        // QRコード用変数
        let lastScannedQR = null;
        let lastScannedTime = 0;
        let isProcessingQR = false;

        // ユーザー名マッピング
        const usernameMapping = {};
        labeledFaceDescriptorsJson.forEach(ld => {
            if (ld.originalUsername) {
                usernameMapping[ld.username] = ld.originalUsername;
            } else {
                usernameMapping[ld.username] = ld.username;
            }
        });

        // デバッグ情報更新
        function updateDebugInfo(info) {
            const timestamp = new Date().toLocaleTimeString();
            debugContent.innerHTML += '<div>' + timestamp + ': ' + info + '</div>';
            debugContent.scrollTop = debugContent.scrollHeight;
        }

        // ステータス更新
        function updateStatus(element, message, type) {
            element.textContent = message;
            element.className = 'status-message status-' + type;
        }

        // カメラセットアップ
        async function setupCamera() {
            try {
                const stream = await navigator.mediaDevices.getUserMedia({
                    video: { width: 640, height: 480, facingMode: 'user' }
                });
                video.srcObject = stream;
                updateDebugInfo('カメラセットアップ完了');
                return new Promise((resolve) => {
                    video.onloadedmetadata = () => {
                        resolve(video);
                    };
                });
            } catch (error) {
                updateDebugInfo('カメラセットアップエラー: ' + error.message);
                throw error;
            }
        }

        // 顔認証初期化
        async function initializeFaceRecognition() {
            updateDebugInfo('顔認証初期化開始');

            if (labeledFaceDescriptorsJson.length > 0) {
                const labeledDescriptors = labeledFaceDescriptorsJson.map(ld => {
                    try {
                        let descriptorArray;
                        if (Array.isArray(ld.descriptor)) {
                            descriptorArray = ld.descriptor;
                        } else if (ld.descriptor && typeof ld.descriptor === 'object') {
                            descriptorArray = Object.values(ld.descriptor);
                        } else {
                            updateDebugInfo('無効な記述子形式: ' + ld.username);
                            return null;
                        }

                        const descriptors = [new Float32Array(descriptorArray)];
                        return new faceapi.LabeledFaceDescriptors(ld.username, descriptors);
                    } catch (error) {
                        updateDebugInfo('デスクリプタ処理エラー (' + ld.username + '): ' + error.message);
                        return null;
                    }
                }).filter(ld => ld !== null);

                updateDebugInfo('有効なデスクリプタ数: ' + labeledDescriptors.length);
                faceMatcher = new faceapi.FaceMatcher(labeledDescriptors, 0.4);
            } else {
                updateStatus(faceStatus, '登録されている顔がありません。', 'warning');
            }

            updateDebugInfo('顔認証初期化完了');
        }

        // QRコードスキャナー初期化
        async function initializeQRScanner() {
            updateDebugInfo('QRコードスキャナー初期化開始');

            html5QrcodeScanner = new Html5Qrcode("video");

            // QRコード検知の設定（自動検知）
            qrDetectionInterval = setInterval(async () => {
                if (!isProcessingQR) {
                    try {
                        const result = await html5QrcodeScanner.scanQrCode(video);
                        if (result) {
                            onQRDetected(result);
                        }
                    } catch (error) {
                        // QRコードが見つからない場合は何もしない
                    }
                }
            }, 500);

            updateDebugInfo('QRコードスキャナー初期化完了');
        }

        // 顔検知処理
        async function processFaceDetection() {
            try {
                detectionCount++;
                const detections = await faceapi.detectAllFaces(video, new faceapi.TinyFaceDetectorOptions({ inputSize: 512, scoreThreshold: 0.5 }))
                    .withFaceLandmarks()
                    .withFaceDescriptors();

                if (detections.length > 0 && faceMatcher) {
                    const currentDescriptor = detections[0].descriptor;
                    const bestMatch = faceMatcher.findBestMatch(currentDescriptor);

                    const FACE_MATCH_THRESHOLD = 0.4;
                    const isAuthenticated = bestMatch.label !== 'unknown' && bestMatch.distance <= FACE_MATCH_THRESHOLD;

                    if (isAuthenticated) {
                        if (lastMatchedUser === bestMatch.label) {
                            consecutiveMatches++;
                        } else {
                            consecutiveMatches = 1;
                            lastMatchedUser = bestMatch.label;
                        }

                        const originalUsername = usernameMapping[bestMatch.label] || bestMatch.label;

                        if (consecutiveMatches >= REQUIRED_CONSECUTIVE_MATCHES) {
                            updateStatus(faceStatus, `認証成功: ${originalUsername}さん`, 'success');
                            faceDetails.innerHTML = `<p>距離: ${bestMatch.distance.toFixed(3)}</p><p>連続マッチ: ${consecutiveMatches}/${REQUIRED_CONSECUTIVE_MATCHES}</p>`;

                            // 自動打刻実行
                            await performFaceCheckin(originalUsername);
                        } else {
                            updateStatus(faceStatus, `認証中: ${originalUsername}さん (${consecutiveMatches}/${REQUIRED_CONSECUTIVE_MATCHES})`, 'warning');
                            faceDetails.innerHTML = `<p>距離: ${bestMatch.distance.toFixed(3)}</p>`;
                        }
                    } else {
                        consecutiveMatches = 0;
                        lastMatchedUser = null;

                        if (bestMatch.label === 'unknown') {
                            updateStatus(faceStatus, '未登録の顔です', 'error');
                        } else {
                            updateStatus(faceStatus, `類似度不足 (距離: ${bestMatch.distance.toFixed(3)})`, 'error');
                        }
                        faceDetails.innerHTML = '';
                    }
                } else {
                    if (detectionCount % 10 === 0) { // 10回に1回更新
                        updateStatus(faceStatus, '顔を検知していません', 'info');
                        faceDetails.innerHTML = '';
                    }
                }
            } catch (error) {
                updateDebugInfo('顔検知エラー: ' + error.message);
            }
        }

        // QRコード検知処理
        function onQRDetected(decodedText) {
            const now = Date.now();

            // 重複スキャンを防ぐ（3秒以内の同じコードは無視）
            if (decodedText === lastScannedQR && (now - lastScannedTime) < 3000) {
                return;
            }

            lastScannedQR = decodedText;
            lastScannedTime = now;

            updateDebugInfo('QRコード検知: ' + decodedText);
            updateStatus(qrStatus, 'QRコードを検知しました', 'success');

            document.getElementById('qr-text').textContent = decodedText;
            document.getElementById('qr-detected').style.display = 'block';
        }

        // 顔認証打刻実行
        async function performFaceCheckin(username) {
            updateDebugInfo('顔認証打刻実行: ' + username);

            try {
                const response = await fetch("${pageContext.request.contextPath}/face/attendance", {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                    body: new URLSearchParams({
                        'action': 'face_checkin',
                        'username': username
                    })
                });

                const data = await response.json();

                if (data.success) {
                    updateStatus(faceStatus, data.message, 'success');
                    updateDebugInfo('顔認証打刻成功: ' + data.user + ' - ' + data.action);

                    // 成功後5秒でリセット
                    setTimeout(() => {
                        consecutiveMatches = 0;
                        lastMatchedUser = null;
                        updateStatus(faceStatus, '顔認証待機中...', 'info');
                        faceDetails.innerHTML = '';
                    }, 5000);
                } else {
                    updateStatus(faceStatus, data.error, 'error');
                    updateDebugInfo('顔認証打刻失敗: ' + data.error);
                }
            } catch (error) {
                updateStatus(faceStatus, '打刻処理でエラーが発生しました', 'error');
                updateDebugInfo('顔認証打刻エラー: ' + error.message);
            }
        }

        // QRコード打刻実行
        async function processQRCheckin() {
            if (isProcessingQR) return;

            isProcessingQR = true;
            const userId = lastScannedQR;

            updateDebugInfo('QRコード打刻実行: ' + userId);
            updateStatus(qrStatus, '打刻処理中...', 'info');

            try {
                const response = await fetch("${pageContext.request.contextPath}/face/attendance", {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                    body: new URLSearchParams({
                        'action': 'qr_checkin',
                        'userId': userId
                    })
                });

                const data = await response.json();

                if (data.success) {
                    updateStatus(qrStatus, data.message, 'success');
                    updateDebugInfo('QRコード打刻成功: ' + data.user + ' - ' + data.action);

                    // 成功後3秒でQR結果をリセット
                    setTimeout(() => {
                        document.getElementById('qr-detected').style.display = 'none';
                        lastScannedQR = null;
                        updateStatus(qrStatus, 'QRコード待機中...', 'info');
                    }, 3000);
                } else {
                    updateStatus(qrStatus, data.error, 'error');
                    updateDebugInfo('QRコード打刻失敗: ' + data.error);
                }
            } catch (error) {
                updateStatus(qrStatus, '打刻処理でエラーが発生しました', 'error');
                updateDebugInfo('QRコード打刻エラー: ' + error.message);
            } finally {
                isProcessingQR = false;
            }
        }

        // デバッグ表示切り替え
        function toggleDebug() {
            const debugInfo = document.getElementById('debug-info');
            debugInfo.style.display = debugInfo.style.display === 'none' ? 'block' : 'none';
        }

        // メイン初期化
        async function initialize() {
            try {
                updateStatus(globalStatus, 'モデルを読み込んでいます...', 'info');
                updateDebugInfo('システム初期化開始');

                // Face-API初期化
                await window.FaceAPIUtils.safeInitialize();
                updateDebugInfo('Face-API初期化完了');

                // モデル読み込み
                const localBaseUrl = '${pageContext.request.contextPath}/models';
                const cdnBaseUrl = 'https://cdn.jsdelivr.net/npm/face-api.js@0.22.2/weights';
                const loadResult = await window.FaceAPIUtils.loadModelsWithFallback(localBaseUrl, cdnBaseUrl);

                if (!loadResult.success) {
                    throw new Error('モデルの読み込みに失敗しました: ' + loadResult.error);
                }

                updateDebugInfo('モデル読み込み完了: ' + loadResult.source);

                // カメラセットアップ
                updateStatus(globalStatus, 'カメラをセットアップしています...', 'info');
                await setupCamera();

                // 顔認証初期化
                await initializeFaceRecognition();

                // QRコードスキャナー初期化
                await initializeQRScanner();

                // ビデオ再生開始
                video.play();

                // 顔検知開始
                faceDetectionInterval = setInterval(processFaceDetection, 1000);

                updateStatus(globalStatus, 'システム準備完了 - カメラに顔を向けたりQRコードをスキャンしてください', 'success');
                updateDebugInfo('システム初期化完了');
                isInitialized = true;

            } catch (error) {
                updateStatus(globalStatus, '初期化エラー: ' + error.message, 'error');
                updateDebugInfo('初期化エラー: ' + error.message);
            }
        }

        // クリーンアップ
        function cleanup() {
            if (faceDetectionInterval) {
                clearInterval(faceDetectionInterval);
            }
            if (qrDetectionInterval) {
                clearInterval(qrDetectionInterval);
            }
            if (video.srcObject) {
                video.srcObject.getTracks().forEach(track => track.stop());
            }
        }

        // ページ離脱時のクリーンアップ
        window.addEventListener('beforeunload', cleanup);

        // 初期化開始
        initialize();
    </script>
</body>
</html>
