<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.example.attendance.dto.User" %>
<%
    // 認証チェック
    User user = (User) request.getAttribute("authenticatedUser");
    if (user == null) {
        user = (User) session.getAttribute("user");
    }
    if (user == null) {
        response.sendRedirect(request.getContextPath() + "/login");
        return;
    }
%>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>顔認証 - 勤怠管理システム</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/style.css">
    <script src="https://cdn.jsdelivr.net/npm/@vladmandic/face-api@1.7.15/dist/face-api.min.js"></script>
    <!-- 初期化スクリプト -->
    <script src="${pageContext.request.contextPath}/js/face-api-init.js"></script>
    <style>
        .face-auth-container {
            max-width: 800px;
            margin: 0 auto;
            padding: 20px;
        }
        .camera-container {
            position: relative;
            display: inline-block;
            margin: 20px 0;
        }
        #video, #canvas {
            border: 2px solid #ddd;
            border-radius: 8px;
            max-width: 100%;
            height: auto;
        }
        .controls {
            margin: 20px 0;
            text-align: center;
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
        .status-warning {
            background-color: #fff3cd;
            color: #856404;
            border: 1px solid #ffeaa7;
        }
        .face-guide {
            background-color: #f8f9fa;
            padding: 15px;
            border-radius: 8px;
            margin: 20px 0;
            border-left: 4px solid #28a745;
        }
        .face-guide h3 {
            margin-top: 0;
            color: #28a745;
        }
        .progress-container {
            margin: 20px 0;
            display: none;
        }
        .progress-bar {
            width: 100%;
            height: 20px;
            background-color: #f0f0f0;
            border-radius: 10px;
            overflow: hidden;
        }
        .progress-fill {
            height: 100%;
            background-color: #28a745;
            width: 0%;
            transition: width 0.3s ease;
        }
        .detection-info {
            font-size: 0.9em;
            color: #666;
            margin-top: 10px;
        }
        .attendance-result {
            background-color: #e9ecef;
            padding: 20px;
            border-radius: 8px;
            margin: 20px 0;
            text-align: center;
            display: none;
        }
        .attendance-result.success {
            background-color: #d4edda;
            border: 2px solid #28a745;
        }
        .attendance-result.error {
            background-color: #f8d7da;
            border: 2px solid #dc3545;
        }
        .result-icon {
            font-size: 3em;
            margin-bottom: 10px;
        }
        .result-details {
            margin-top: 15px;
            font-size: 0.9em;
            color: #666;
        }
        .btn-large {
            padding: 12px 24px;
            font-size: 1.1em;
            margin: 0 10px;
        }
        .registration-status {
            background-color: #f8f9fa;
            padding: 10px;
            border-radius: 4px;
            margin: 10px 0;
            text-align: center;
        }
        .registration-status.registered {
            background-color: #d4edda;
            color: #155724;
        }
        .registration-status.not-registered {
            background-color: #fff3cd;
            color: #856404;
        }
    </style>
</head>
<body>
    <div class="face-auth-container">
        <h1>顔認証</h1>
        <p>カメラの前に顔を向け、自動で出退勤を記録します。</p>

        <!-- 顔登録ステータス -->
        <div id="registration-status" class="registration-status">
            顔登録状況を確認中...
        </div>

        <div class="face-guide">
            <h3>🔒 認証の流れ</h3>
            <ol>
                <li>カメラを開始して顔を検出します</li>
                <li>登録済みの顔と一致するか確認します</li>
                <li>一致した場合、自動で出退勤を記録します</li>
                <li>認証結果が画面に表示されます</li>
            </ol>
        </div>

        <div class="camera-container">
            <video id="video" width="640" height="480" autoplay muted></video>
            <canvas id="canvas" width="640" height="480" style="display: none;"></canvas>
        </div>

        <div class="detection-info">
            <div id="detection-status">モデルを読み込み中...</div>
            <div id="face-count">検出された顔: 0</div>
            <div id="auth-status"></div>
        </div>

        <div class="progress-container" id="progress-container">
            <div class="progress-bar">
                <div class="progress-fill" id="progress-fill"></div>
            </div>
            <p id="progress-text">認証処理中...</p>
        </div>

        <div class="controls">
            <button id="start-auth" class="btn-primary btn-large">認証を開始</button>
            <button id="stop-auth" class="btn-secondary" disabled>停止</button>
        </div>

        <div id="status-message"></div>

        <!-- 認証結果表示 -->
        <div id="attendance-result" class="attendance-result">
            <div id="result-icon"></div>
            <h3 id="result-title"></h3>
            <p id="result-message"></p>
            <div id="result-details" class="result-details"></div>
        </div>

        <div class="controls" style="margin-top: 30px;">
            <a href="${pageContext.request.contextPath}/attendance" class="btn-secondary">勤怠管理に戻る</a>
            <a href="${pageContext.request.contextPath}/face/register" class="btn-primary">顔登録ページへ</a>
        </div>
    </div>

    <script>
        let video = document.getElementById('video');
        let canvas = document.getElementById('canvas');
        let ctx = canvas.getContext('2d');
        let stream = null;
        let faceDetectionInterval = null;
        let modelsLoaded = false;
        let isAuthenticating = false;
        let lastAuthTime = 0;
        const AUTH_COOLDOWN = 5000; // 5秒間のクールダウン

        // DOM要素
        const startAuthBtn = document.getElementById('start-auth');
        const stopAuthBtn = document.getElementById('stop-auth');
        const statusMessage = document.getElementById('status-message');
        const detectionStatus = document.getElementById('detection-status');
        const faceCount = document.getElementById('face-count');
        const authStatus = document.getElementById('auth-status');
        const progressContainer = document.getElementById('progress-container');
        const progressFill = document.getElementById('progress-fill');
        const progressText = document.getElementById('progress-text');
        const attendanceResult = document.getElementById('attendance-result');
        const resultIcon = document.getElementById('result-icon');
        const resultTitle = document.getElementById('result-title');
        const resultMessage = document.getElementById('result-message');
        const resultDetails = document.getElementById('result-details');
        const registrationStatus = document.getElementById('registration-status');

        // 顔登録状況チェック
        async function checkRegistrationStatus() {
            try {
                const response = await fetch('${pageContext.request.contextPath}/face/status', {
                    credentials: 'same-origin'
                });
                const result = await response.json();

                if (result.success) {
                    if (result.data.isRegistered) {
                        registrationStatus.textContent = '✓ 顔が登録されています';
                        registrationStatus.className = 'registration-status registered';
                    } else {
                        registrationStatus.textContent = '⚠ 顔が登録されていません。先に顔登録を行ってください';
                        registrationStatus.className = 'registration-status not-registered';
                        startAuthBtn.disabled = true;
                    }
                } else {
                    registrationStatus.textContent = '顔登録状況の確認に失敗しました';
                    registrationStatus.className = 'registration-status not-registered';
                }
            } catch (error) {
                console.error('Registration status check error:', error);
                registrationStatus.textContent = '顔登録状況の確認に失敗しました';
                registrationStatus.className = 'registration-status not-registered';
            }
        }

        // モデル読み込み (face-api-init.js を使用)
        async function loadModels() {
            try {
                showStatus('ライブラリを初期化中...', 'info');

                // 重複読み込みを防ぐ
                if (modelsLoaded) {
                    console.log('Models already loaded');
                    showStatus('モデル読み込み完了', 'success');
                    detectionStatus.textContent = '認証を開始してください';
                    return;
                }

                // 安全な初期化
                console.log('Starting safe initialization...');
                const initResult = await window.FaceAPIUtils.safeInitialize();
                
                if (!initResult.success) {
                    throw new Error('ライブラリの初期化に失敗しました');
                }

                showStatus('モデルを読み込み中...', 'info');

                // モデル読み込み
                const localBaseUrl = '${pageContext.request.contextPath}/models/';
                const cdnBaseUrl = 'https://cdn.jsdelivr.net/npm/@vladmandic/face-api@1.7.15/model/';

                console.log('Loading models...');
                const loadResult = await window.FaceAPIUtils.loadModelsWithFallback(localBaseUrl, cdnBaseUrl);
                
                if (!loadResult.success) {
                    throw new Error(`モデル読み込み失敗: ${loadResult.error}`);
                }

                modelsLoaded = true;
                showStatus('モデル読み込み完了', 'success');
                detectionStatus.textContent = '認証を開始してください';
                
                console.log('✓ All models loaded successfully');
                console.log('State:', window.FaceAPIUtils.getState());

            } catch (error) {
                console.error('Model loading error:', error);
                showStatus('モデル読み込みに失敗しました: ' + (error.message || error), 'error');
                detectionStatus.textContent = 'モデル読み込みエラー';
                modelsLoaded = false;
                
                // 詳細なデバッグ情報を出力
                console.log('Debug info:', {
                    tf: typeof tf,
                    faceapi: typeof faceapi,
                    FaceAPIUtils: typeof window.FaceAPIUtils,
                    state: window.FaceAPIUtils ? window.FaceAPIUtils.getState() : 'unavailable'
                });
            }
        }

        // 認証開始
        async function startAuthentication() {
            try {
                if (!modelsLoaded) {
                    showStatus('モデルが読み込まれていません', 'error');
                    return;
                }

                // 顔登録状況を確認
                await checkRegistrationStatus();
                if (registrationStatus.classList.contains('not-registered')) {
                    showStatus('顔が登録されていないため認証を開始できません', 'warning');
                    return;
                }

                stream = await navigator.mediaDevices.getUserMedia({
                    video: {
                        width: 640,
                        height: 480,
                        facingMode: 'user'
                    }
                });

                video.srcObject = stream;
                video.style.display = 'block';
                canvas.style.display = 'none';

                startAuthBtn.disabled = true;
                stopAuthBtn.disabled = false;
                isAuthenticating = true;

                showStatus('認証を開始しました', 'success');
                detectionStatus.textContent = '顔検出を開始しています...';
                authStatus.textContent = '';

                // 顔検出を開始
                startFaceDetection();

            } catch (error) {
                console.error('カメラアクセスエラー:', error);
                showStatus('カメラアクセスに失敗しました: ' + error.message, 'error');
            }
        }

        // 認証停止
        function stopAuthentication() {
            if (stream) {
                stream.getTracks().forEach(track => track.stop());
                stream = null;
            }

            video.style.display = 'none';
            canvas.style.display = 'none';

            startAuthBtn.disabled = false;
            stopAuthBtn.disabled = true;
            isAuthenticating = false;

            stopFaceDetection();
            showStatus('認証を停止しました', 'info');
            detectionStatus.textContent = '認証が停止しました';
            faceCount.textContent = '検出された顔: 0';
            authStatus.textContent = '';
        }

        // 顔検出開始
        function startFaceDetection() {
            stopFaceDetection(); // 念のため停止

            faceDetectionInterval = setInterval(async () => {
                if (!modelsLoaded || !isAuthenticating || video.videoWidth === 0) return;

                try {
                    const detections = await safeDetectFaces(video);

                    faceCount.textContent = `検出された顔: ${detections.length}`;

                    if (detections.length === 1) {
                        detectionStatus.textContent = '顔を検出しました。認証を開始します...';
                        authStatus.textContent = '認証処理中...';

                        // クールダウンチェック
                        const now = Date.now();
                        if (now - lastAuthTime < AUTH_COOLDOWN) {
                            authStatus.textContent = '認証クールダウン中...';
                            return;
                        }

                        // 顔認証を実行
                        await performAuthentication(detections[0]);
                        lastAuthTime = now;

                    } else if (detections.length === 0) {
                        detectionStatus.textContent = '顔が検出されません。正面向きで撮影してください';
                        authStatus.textContent = '';
                    } else {
                        detectionStatus.textContent = `複数の顔が検出されました (${detections.length}人)。1人だけにしてください`;
                        authStatus.textContent = '';
                    }

                } catch (error) {
                    console.error('顔検出エラー:', error);
                    detectionStatus.textContent = '顔検出エラー: ' + error.message;
                    authStatus.textContent = '';
                }
            }, 200); // 検出間隔を200msに設定
        }

        // 顔検出停止
        function stopFaceDetection() {
            if (faceDetectionInterval) {
                clearInterval(faceDetectionInterval);
                faceDetectionInterval = null;
            }
        }

        // 安全な顔検出関数（register.jsp と同一の防御実装）
        async function safeDetectFaces(input) {
            try {
                // TensorFlow.jsとface-api.jsの状態確認
                if (typeof tf === 'undefined' || !tf.getBackend) {
                    throw new Error('TensorFlow.js backend not ready');
                }

                if (typeof faceapi === 'undefined' || !faceapi.detectAllFaces) {
                    throw new Error('face-api.js not ready');
                }

                const options = new faceapi.TinyFaceDetectorOptions({ inputSize: 512, scoreThreshold: 0.5 });

                // 最初にチェーン呼び出しが可能かどうかをチェックして安全に実行
                try {
                    const chain = faceapi.detectAllFaces(input, options);
                    console.log('detectAllFaces returned:', typeof chain);

                    if (chain && typeof chain.withFaceLandmarks === 'function' && typeof chain.withFaceDescriptors === 'function') {
                        // 通常のチェーン呼び出し
                        const detections = await chain.withFaceLandmarks().withFaceDescriptors();
                        return detections || [];
                    } else {
                        // チェーンメソッドが存在しない場合はログを出して詳細エラーを投げる
                        console.warn('face-api chaining methods missing', {
                            withFaceLandmarks: chain && typeof chain.withFaceLandmarks,
                            withFaceDescriptors: chain && typeof chain.withFaceDescriptors,
                            faceapi_detectAllFaces_type: typeof faceapi.detectAllFaces
                        });
                        throw new Error('face-api chaining methods not available; possible version mismatch or duplicate library load');
                    }
                } catch (chainError) {
                    console.error('Chained face detection failed:', chainError);
                    // 追加の診断情報を出力して、根本原因の特定を容易にする
                    console.log('Diagnostic info:', {
                        tf: typeof tf,
                        tfBackend: (typeof tf !== 'undefined' && tf.getBackend) ? tf.getBackend() : 'unavailable',
                        faceapi: typeof faceapi,
                        faceapi_nets: faceapi && faceapi.nets ? Object.keys(faceapi.nets) : 'unavailable',
                        faceapi_detectAllFaces: String(faceapi.detectAllFaces).slice(0,200)
                    });

                    // 明確な情報付きで再送出
                    throw new Error('Face detection chain failed: ' + (chainError && chainError.message ? chainError.message : chainError));
                }

            } catch (error) {
                console.error('Safe detect faces error:', error);
                if (error && error.message && error.message.includes('not a function')) {
                    console.warn('Function call error detected, consider checking for multiple face-api/tfjs versions loaded or incompatible versions');
                }
                throw error;
            }
        }

        // 顔認証実行
        async function performAuthentication(detection) {
            try {
                progressContainer.style.display = 'block';
                progressFill.style.width = '0%';
                progressText.textContent = '顔特徴を抽出しています...';

                const faceDescriptor = detection.descriptor;

                progressFill.style.width = '50%';
                progressText.textContent = '認証サーバーに送信しています...';

                // サーバーに認証リクエストを送信
                const formData = new FormData();
                formData.append('faceDescriptor', JSON.stringify(Array.from(faceDescriptor)));

                const response = await fetch('${pageContext.request.contextPath}/face/authenticate', {
                    method: 'POST',
                    body: formData,
                    credentials: 'same-origin'
                });

                const result = await response.json();

                progressFill.style.width = '100%';
                progressText.textContent = '認証完了';

                if (result.success) {
                    showAttendanceResult('success', '✅', '認証成功', result.message, result.data);
                    showStatus('認証成功！勤怠が記録されました', 'success');
                } else {
                    showAttendanceResult('error', '❌', '認証失敗', result.error || '認証に失敗しました', null);
                    showStatus('認証失敗: ' + (result.error || '不明なエラー'), 'error');
                }

            } catch (error) {
                console.error('認証エラー:', error);
                showAttendanceResult('error', '❌', '認証エラー', '認証処理中にエラーが発生しました', null);
                showStatus('認証エラー: ' + error.message, 'error');
            } finally {
                setTimeout(() => {
                    progressContainer.style.display = 'none';
                    attendanceResult.style.display = 'none';
                }, 3000);
            }
        }

        // 勤怠結果表示
        function showAttendanceResult(type, icon, title, message, data) {
            attendanceResult.className = `attendance-result ${type}`;
            resultIcon.textContent = icon;
            resultTitle.textContent = title;
            resultMessage.textContent = message;

            if (data) {
                let details = '';
                if (data.action) details += `アクション: ${data.action}<br>`;
                if (data.recognizedUsername) details += `ユーザー: ${data.recognizedUsername}<br>`;
                if (data.confidence) details += `信頼度: ${(data.confidence * 100).toFixed(1)}%<br>`;
                if (data.timestamp) details += `時刻: ${data.timestamp}`;
                resultDetails.innerHTML = details;
            } else {
                resultDetails.innerHTML = '';
            }

            attendanceResult.style.display = 'block';
        }

        // ステータスメッセージ表示
        function showStatus(message, type) {
            statusMessage.className = 'status-message status-' + type;
            statusMessage.textContent = message;
            statusMessage.style.display = 'block';
        }

        // イベントリスナー
        startAuthBtn.addEventListener('click', startAuthentication);
        stopAuthBtn.addEventListener('click', stopAuthentication);

        // ページ読み込み時に初期化
        document.addEventListener('DOMContentLoaded', function() {
            console.log('DOM Content Loaded');
            
            // ライブラリの読み込み完了を確実に待つ
            function waitForLibrariesAndLoad() {
                const checkInterval = 100; // 100ms間隔でチェック
                const maxWaitTime = 10000; // 最大10秒待機
                let waitTime = 0;
                
                const checkLibraries = () => {
                    // 必要なオブジェクトの存在確認
                    const tfReady = typeof tf !== 'undefined' && tf.version;
                    const faceapiReady = typeof faceapi !== 'undefined' && faceapi.nets;
                    const utilsReady = typeof window.FaceAPIUtils !== 'undefined';
                    
                    console.log('Library check:', {
                        tf: tfReady,
                        faceapi: faceapiReady,
                        utils: utilsReady,
                        waitTime: waitTime
                    });
                    
                    if (tfReady && faceapiReady && utilsReady) {
                        console.log('All libraries ready, starting initialization...');
                        loadModels();
                        checkRegistrationStatus();
                        return;
                    }
                    
                    waitTime += checkInterval;
                    if (waitTime >= maxWaitTime) {
                        console.error('Library loading timeout');
                        showStatus('ライブラリの読み込みがタイムアウトしました', 'error');
                        return;
                    }
                    
                    setTimeout(checkLibraries, checkInterval);
                };
                
                checkLibraries();
            }
            
            waitForLibrariesAndLoad();
        });

        // ページ離脱時にカメラを停止
        window.addEventListener('beforeunload', stopAuthentication);
    </script>
</body>
</html>
