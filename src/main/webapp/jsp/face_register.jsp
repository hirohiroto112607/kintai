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
    <title>顔登録 - 勤怠管理システム</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/style.css">
    <!-- TensorFlow.js を先に読み込み（互換性の良いバージョン） -->
    <script>
        // TensorFlow.jsの重複警告を抑制
        window.tf = window.tf || undefined;
    </script>
    <script src="https://cdn.jsdelivr.net/npm/@tensorflow/tfjs@3.21.0/dist/tf.min.js"></script>
    <!-- face-api.js を後に読み込み（互換バージョン） -->
    <script>
        // face-api.jsの重複警告を抑制
        window.faceapi = window.faceapi || undefined;
    </script>
    <script src="https://cdn.jsdelivr.net/npm/face-api.js@0.22.2/dist/face-api.min.js"></script>
    <!-- 初期化スクリプト -->
    <script src="${pageContext.request.contextPath}/js/face-api-init.js"></script>
    <style>
        .face-register-container {
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
        .face-guide {
            background-color: #f8f9fa;
            padding: 15px;
            border-radius: 8px;
            margin: 20px 0;
            border-left: 4px solid #007bff;
        }
        .face-guide h3 {
            margin-top: 0;
            color: #007bff;
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
            background-color: #007bff;
            width: 0%;
            transition: width 0.3s ease;
        }
        .detection-info {
            font-size: 0.9em;
            color: #666;
            margin-top: 10px;
        }
    </style>
</head>
<body>
    <div class="face-register-container">
        <h1>顔登録</h1>
        <p>勤怠管理で使用する顔を登録します。カメラの前に正面向いて、顔が枠内に収まるようにしてください。</p>

        <div class="face-guide">
            <h3>📸 撮影のコツ</h3>
            <ul>
                <li>顔が画面の中央に収まるように位置を調整してください</li>
                <li>明るい場所で撮影すると認識精度が向上します</li>
                <li>眼鏡や帽子は外して撮影してください</li>
                <li>表情は自然な状態で撮影してください</li>
            </ul>
        </div>

        <div class="camera-container">
            <video id="video" width="640" height="480" autoplay muted></video>
            <canvas id="canvas" width="640" height="480" style="display: none;"></canvas>
        </div>

        <div class="detection-info">
            <div id="detection-status">モデルを読み込み中...</div>
            <div id="face-count">検出された顔: 0</div>
        </div>

        <div class="progress-container" id="progress-container">
            <div class="progress-bar">
                <div class="progress-fill" id="progress-fill"></div>
            </div>
            <p id="progress-text">顔を検出しています...</p>
        </div>

        <div class="controls">
            <button id="start-camera" class="btn-primary">カメラを開始</button>
            <button id="capture-face" class="btn-success" disabled>顔を登録</button>
            <button id="stop-camera" class="btn-secondary" disabled>停止</button>
        </div>

        <div id="status-message"></div>

        <div class="controls" style="margin-top: 30px;">
            <a href="${pageContext.request.contextPath}/attendance" class="btn-secondary">勤怠管理に戻る</a>
            <a href="${pageContext.request.contextPath}/face/authenticate" class="btn-primary">顔認証ページへ</a>
        </div>
    </div>

    <script>
        let video = document.getElementById('video');
        let canvas = document.getElementById('canvas');
        let ctx = canvas.getContext('2d');
        let stream = null;
        let faceDetectionInterval = null;
        let modelsLoaded = false;

        // DOM要素
        const startCameraBtn = document.getElementById('start-camera');
        const captureFaceBtn = document.getElementById('capture-face');
        const stopCameraBtn = document.getElementById('stop-camera');
        const statusMessage = document.getElementById('status-message');
        const detectionStatus = document.getElementById('detection-status');
        const faceCount = document.getElementById('face-count');
        const progressContainer = document.getElementById('progress-container');
        const progressFill = document.getElementById('progress-fill');
        const progressText = document.getElementById('progress-text');

         // モデル読み込み
        async function loadModels() {
            try {
                showStatus('ライブラリを初期化中...', 'info');

                // 重複読み込みを防ぐ
                if (modelsLoaded) {
                    console.log('Models already loaded');
                    showStatus('モデル読み込み完了', 'success');
                    detectionStatus.textContent = 'カメラを開始してください';
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
                const cdnBaseUrl = 'https://cdn.jsdelivr.net/npm/face-api.js@0.22.2/weights/';

                console.log('Loading models...');
                const loadResult = await window.FaceAPIUtils.loadModelsWithFallback(localBaseUrl, cdnBaseUrl);
                
                if (!loadResult.success) {
                    throw new Error(`モデル読み込み失敗: ${loadResult.error}`);
                }

                modelsLoaded = true;
                showStatus('モデル読み込み完了', 'success');
                detectionStatus.textContent = 'カメラを開始してください';
                
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

        // カメラ開始
        async function startCamera() {
            try {
                if (!modelsLoaded) {
                    showStatus('モデルが読み込まれていません', 'error');
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

                startCameraBtn.disabled = true;
                captureFaceBtn.disabled = true;
                stopCameraBtn.disabled = false;

                showStatus('カメラを開始しました', 'success');
                detectionStatus.textContent = '顔検出を開始しています...';

                // 顔検出を開始
                startFaceDetection();

            } catch (error) {
                console.error('カメラアクセスエラー:', error);
                showStatus('カメラアクセスに失敗しました: ' + error.message, 'error');
            }
        }

        // カメラ停止
        function stopCamera() {
            if (stream) {
                stream.getTracks().forEach(track => track.stop());
                stream = null;
            }

            video.style.display = 'none';
            canvas.style.display = 'none';

            startCameraBtn.disabled = false;
            captureFaceBtn.disabled = true;
            stopCameraBtn.disabled = true;

            stopFaceDetection();
            showStatus('カメラを停止しました', 'info');
            detectionStatus.textContent = 'カメラが停止しました';
            faceCount.textContent = '検出された顔: 0';
        }

        // 顔検出開始（安全性向上版）
        function startFaceDetection() {
            stopFaceDetection(); // 念のため停止

            faceDetectionInterval = setInterval(async () => {
                if (!modelsLoaded || video.videoWidth === 0) return;

                try {
                    // より安全な顔検出の実行
                    const detections = await safeDetectFaces(video);

                    faceCount.textContent = `検出された顔: ${detections.length}`;

                    if (detections.length === 1) {
                        detectionStatus.textContent = '顔が検出されました！登録ボタンをクリックしてください';
                        captureFaceBtn.disabled = false;

                        // 検出された顔に枠を描画
                        drawFaceDetection(detections[0]);
                    } else if (detections.length === 0) {
                        detectionStatus.textContent = '顔が検出されません。正面向きで撮影してください';
                        captureFaceBtn.disabled = true;
                    } else {
                        detectionStatus.textContent = `複数の顔が検出されました (${detections.length}人)。1人だけにしてください`;
                        captureFaceBtn.disabled = true;
                    }

                } catch (error) {
                    console.error('顔検出エラー:', error);
                    detectionStatus.textContent = '顔検出エラー: ' + error.message;
                    // エラーが続く場合は検出を一時停止
                    if (error.message.includes('not a function')) {
                        console.warn('Face detection function error - restarting detection');
                        stopFaceDetection();
                        setTimeout(startFaceDetection, 2000); // 2秒後に再開
                    }
                }
            }, 100);
        }

        // 安全な顔検出関数
        async function safeDetectFaces(input) {
            try {
                // TensorFlow.jsとface-api.jsの状態確認
                if (!tf || !tf.getBackend()) {
                    throw new Error('TensorFlow.js backend not ready');
                }

                if (!faceapi || !faceapi.detectAllFaces) {
                    throw new Error('face-api.js not ready');
                }

                // 顔検出の実行
                const detections = await faceapi.detectAllFaces(
                    input,
                    new faceapi.TinyFaceDetectorOptions({ 
                        inputSize: 512, 
                        scoreThreshold: 0.5 
                    })
                ).withFaceLandmarks().withFaceDescriptors();

                return detections || [];
            } catch (error) {
                console.error('Safe detect faces error:', error);
                
                // エラーの種類に応じた処理
                if (error.message.includes('not a function')) {
                    // 関数呼び出しエラー - ライブラリの再初期化が必要かも
                    console.warn('Function call error detected, may need re-initialization');
                }
                
                throw error;
            }
        }

        // 顔検出停止
        function stopFaceDetection() {
            if (faceDetectionInterval) {
                clearInterval(faceDetectionInterval);
                faceDetectionInterval = null;
            }
        }

        // 顔検出結果を描画
        function drawFaceDetection(detection) {
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

            const box = detection.detection.box;
            ctx.strokeStyle = '#00ff00';
            ctx.lineWidth = 3;
            ctx.strokeRect(box.x, box.y, box.width, box.height);

            // ランドマークを描画
            const landmarks = detection.landmarks;
            ctx.fillStyle = '#ff0000';
            landmarks.positions.forEach(point => {
                ctx.beginPath();
                ctx.arc(point.x, point.y, 2, 0, 2 * Math.PI);
                ctx.fill();
            });
        }

        // 顔登録（安全性向上版）
        async function captureFace() {
            if (!modelsLoaded) {
                showStatus('モデルが読み込まれていません', 'error');
                return;
            }

            try {
                showStatus('顔を処理中...', 'info');
                captureFaceBtn.disabled = true;
                progressContainer.style.display = 'block';
                progressFill.style.width = '0%';
                progressText.textContent = '顔特徴を抽出しています...';

                // 安全な顔検出と特徴抽出
                const detections = await safeDetectFaces(video);

                if (detections.length !== 1) {
                    throw new Error(`顔が正しく検出されませんでした (検出数: ${detections.length})`);
                }

                const detection = detections[0];
                
                // 顔特徴ベクトルの検証
                if (!detection.descriptor || detection.descriptor.length === 0) {
                    throw new Error('顔特徴の抽出に失敗しました');
                }

                console.log('Face descriptor extracted:', {
                    length: detection.descriptor.length,
                    type: typeof detection.descriptor,
                    isArray: Array.isArray(detection.descriptor)
                });

                const faceDescriptor = detection.descriptor;

                progressFill.style.width = '50%';
                progressText.textContent = '画像をキャプチャしています...';

                // キャンバスに現在のフレームを描画
                ctx.clearRect(0, 0, canvas.width, canvas.height);
                ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

                // 画像をBase64に変換
                const imageData = canvas.toDataURL('image/jpeg', 0.8);

                progressFill.style.width = '80%';
                progressText.textContent = 'サーバーに送信しています...';

                // サーバーに送信するデータを準備
                const formData = new FormData();
                
                // 顔特徴ベクトルを配列として送信（より安全な変換）
                let descriptorArray;
                try {
                    if (faceDescriptor instanceof Float32Array) {
                        descriptorArray = Array.from(faceDescriptor);
                    } else if (Array.isArray(faceDescriptor)) {
                        descriptorArray = faceDescriptor;
                    } else {
                        throw new Error('Unsupported descriptor format');
                    }
                } catch (conversionError) {
                    console.error('Descriptor conversion error:', conversionError);
                    throw new Error('顔特徴データの変換に失敗しました');
                }

                console.log('Descriptor conversion successful:', {
                    originalLength: faceDescriptor.length,
                    arrayLength: descriptorArray.length,
                    firstFewValues: descriptorArray.slice(0, 5)
                });

                formData.append('faceDescriptor', JSON.stringify(descriptorArray));
                formData.append('confidenceThreshold', '0.6');

                // 画像データをBlobに変換して追加
                try {
                    const response = await fetch(imageData);
                    const blob = await response.blob();
                    formData.append('faceImage', blob, 'face.jpg');
                } catch (imgError) {
                    console.warn('Image processing warning:', imgError);
                    // 画像の処理に失敗しても続行
                }

                console.log('Sending face data:', {
                    descriptorLength: descriptorArray.length,
                    confidenceThreshold: 0.6
                });

                const registerResponse = await fetch('${pageContext.request.contextPath}/face/register', {
                    method: 'POST',
                    body: formData,
                    credentials: 'same-origin'
                });

                const result = await registerResponse.json();

                progressFill.style.width = '100%';
                progressText.textContent = '完了';

                if (registerResponse.ok && result.success) {
                    showStatus('顔登録が完了しました！', 'success');
                    setTimeout(() => {
                        window.location.href = '${pageContext.request.contextPath}/face/authenticate';
                    }, 2000);
                } else {
                    const errorMsg = result.error || `HTTP ${registerResponse.status}: ${registerResponse.statusText}`;
                    showStatus('顔登録に失敗しました: ' + errorMsg, 'error');
                    console.error('Server response:', result);
                }

            } catch (error) {
                console.error('顔登録エラー:', error);
                showStatus('顔登録に失敗しました: ' + error.message, 'error');
                
                // エラーの詳細ログ
                console.log('Error details:', {
                    message: error.message,
                    stack: error.stack,
                    tfBackend: tf ? tf.getBackend() : 'undefined',
                    faceapiReady: typeof faceapi !== 'undefined'
                });
            } finally {
                captureFaceBtn.disabled = false;
                setTimeout(() => {
                    progressContainer.style.display = 'none';
                }, 1000);
            }
        }

        // ステータスメッセージ表示
        function showStatus(message, type) {
            statusMessage.className = 'status-message status-' + type;
            statusMessage.textContent = message;
            statusMessage.style.display = 'block';
        }

        // イベントリスナー
        startCameraBtn.addEventListener('click', startCamera);
        stopCameraBtn.addEventListener('click', stopCamera);
        captureFaceBtn.addEventListener('click', captureFace);

        // ページ読み込み時にモデルを読み込み
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
        window.addEventListener('beforeunload', stopCamera);
    </script>
</body>
</html>
