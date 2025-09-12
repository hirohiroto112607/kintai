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
    <title>顔認証 - 勤怠管理システム</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/style.css">
        <script src="https://cdn.jsdelivr.net/npm/@tensorflow/tfjs@1.7.4/dist/tf.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/face-api.js@0.22.2/dist/face-api.min.js"></script>
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
    </style>
</head>
<body>
    <div class="face-auth-container">
        <h1>顔認証</h1>
        <p>カメラに顔を向けてログインしてください。</p>

        <div class="camera-container">
            <video id="video" width="640" height="480" autoplay muted></video>
            <canvas id="canvas"></canvas>
        </div>

        <div id="status-message"></div>
        
        <div id="debug-info" style="background: #f8f9fa; padding: 10px; margin: 10px 0; border-radius: 4px; font-family: monospace; font-size: 12px; display: none;">
            <h4>デバッグ情報:</h4>
            <div id="debug-content"></div>
        </div>

        <div class="controls">
            <button id="manual-login-btn" style="display: none; margin: 10px; padding: 10px 20px; background: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer;">手動ログイン</button>
            <button id="toggle-debug" style="margin: 10px; padding: 5px 10px; background: #6c757d; color: white; border: none; border-radius: 4px; cursor: pointer;">デバッグ情報表示</button>
            <button id="restart-detection" style="margin: 10px; padding: 5px 10px; background: #28a745; color: white; border: none; border-radius: 4px; cursor: pointer;">認証再開</button>
            <a href="${pageContext.request.contextPath}/login.jsp">通常のログインに戻る</a>
        </div>
    </div>

    <script>
        const video = document.getElementById('video');
        const statusMessage = document.getElementById('status-message');
        const debugInfo = document.getElementById('debug-info');
        const debugContent = document.getElementById('debug-content');
        const manualLoginBtn = document.getElementById('manual-login-btn');
        const toggleDebugBtn = document.getElementById('toggle-debug');
        const restartDetectionBtn = document.getElementById('restart-detection');
        
        const labeledFaceDescriptorsJson = JSON.parse('<%= faceDescriptorsJson %>');
        let faceMatcher;
        let detectionInterval;
        let lastDetectedUser = null;
        let detectionCount = 0;
        let consecutiveMatches = 0;  // 連続マッチ回数
        let lastMatchedUser = null;  // 最後にマッチしたユーザー
        const REQUIRED_CONSECUTIVE_MATCHES = 3; // 認証に必要な連続マッチ回数
        
        // ユーザー名マッピング: uniqueLabel -> originalUsername
        const usernameMapping = {};
        labeledFaceDescriptorsJson.forEach(ld => {
            if (ld.originalUsername) {
                usernameMapping[ld.username] = ld.originalUsername;
            } else {
                usernameMapping[ld.username] = ld.username; // フォールバック
            }
        });
        console.log('Username mapping:', usernameMapping);
        
        // デバッグ情報を更新する関数
        function updateDebugInfo(info) {
            const timestamp = new Date().toLocaleTimeString();
            debugContent.innerHTML += '<div>' + timestamp + ': ' + info + '</div>';
            debugContent.scrollTop = debugContent.scrollHeight;
        }

        async function setupCamera() {
            const stream = await navigator.mediaDevices.getUserMedia({ video: {} });
            video.srcObject = stream;
            return new Promise((resolve) => {
                video.onloadedmetadata = () => {
                    resolve(video);
                };
            });
        }

        function stopCamera() {
            if (video.srcObject) {
                video.srcObject.getTracks().forEach(track => track.stop());
            }
            if (detectionInterval) {
                clearInterval(detectionInterval);
            }
        }
        
        // ログイン処理を関数として分離
        async function performLogin(username) {
            console.log('performLogin called with username:', username);
            console.log('username type:', typeof username);
            console.log('username length:', username ? username.length : 'null/undefined');
            
            updateDebugInfo('ログイン試行: ' + username);
            
            // URLSearchParamsを使用してapplication/x-www-form-urlencodedで送信
            const params = new URLSearchParams();
            params.append('username', username);
            
            // リクエストパラメータをログ出力
            console.log('Request params:', params.toString());
            
            try {
                const response = await fetch("${pageContext.request.contextPath}/face/login", {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                    body: params
                });
                
                updateDebugInfo('レスポンス status: ' + response.status);
                console.log('Response status:', response.status);
                console.log('Response headers:', response.headers);
                
                if (response.ok) {
                    updateDebugInfo('ログイン成功 - リダイレクト中...');
                    window.location.href = "${pageContext.request.contextPath}/attendance";
                } else {
                    statusMessage.className = 'status-message status-error';
                    statusMessage.textContent = 'ログインに失敗しました。';
                    updateDebugInfo('ログイン失敗: ' + response.status);
                    
                    // レスポンスボディも確認
                    const responseText = await response.text();
                    console.log('Error response body:', responseText);
                }
            } catch (error) {
                console.error('Login error:', error);
                updateDebugInfo('ログインエラー: ' + error.message);
                statusMessage.className = 'status-message status-error';
                statusMessage.textContent = 'ログイン処理でエラーが発生しました。';
            }
        }

        async function onPlay() {
            updateDebugInfo('登録済み顔データ数: ' + labeledFaceDescriptorsJson.length);
            
            if (labeledFaceDescriptorsJson.length > 0) {
                // 登録済みデータの型確認と変換
                const labeledDescriptors = labeledFaceDescriptorsJson.map(ld => {
                    try {
                        // 登録済みデータの型確認と変換
                        let descriptorArray;
                        if (Array.isArray(ld.descriptor)) {
                            // データベースから直接読み込まれた場合（配列）
                            descriptorArray = ld.descriptor;
                        } else if (ld.descriptor && typeof ld.descriptor === 'object') {
                            // オブジェクト形式の場合は値を取得
                            descriptorArray = Object.values(ld.descriptor);
                        } else {
                            console.error('Invalid descriptor format for user:', ld.username);
                            updateDebugInfo('無効な記述子形式: ' + ld.username);
                            return null;
                        }
                        
                        // 単一の特徴ベクトルとして扱う（配列の配列ではなく）
                        const descriptors = [new Float32Array(descriptorArray)];
                        updateDebugInfo('ユーザー' + ld.username + 'のデスクリプタ読み込み: 長さ=' + descriptors[0].length);
                        
                        // デスクリプタの最初の数値を表示（デバッグ用）
                        const firstValues = Array.from(descriptors[0].slice(0, 5)).map(v => v.toFixed(3)).join(', ');
                        console.log('User ' + ld.username + ' descriptor sample:', firstValues);
                        
                        return new faceapi.LabeledFaceDescriptors(ld.username, descriptors);
                    } catch (error) {
                        console.error('Error processing descriptor for user:', ld.username, error);
                        updateDebugInfo('デスクリプタ処理エラー (' + ld.username + '): ' + error.message);
                        return null;
                    }
                }).filter(ld => ld !== null);
                
                updateDebugInfo('有効なデスクリプタ数: ' + labeledDescriptors.length);
                // 顔認証の閾値を0.4に設定（0.4以下で同一人物と判定）
                faceMatcher = new faceapi.FaceMatcher(labeledDescriptors, 0.4);
            } else {
                statusMessage.className = 'status-message status-error';
                statusMessage.textContent = '登録されている顔がありません。';
                updateDebugInfo('登録済み顔データが見つかりません');
                return;
            }

            detectionInterval = setInterval(async () => {
                try {
                    detectionCount++;
                    const detections = await faceapi.detectAllFaces(video, new faceapi.TinyFaceDetectorOptions({ inputSize: 512, scoreThreshold: 0.5 })).withFaceLandmarks().withFaceDescriptors(); // faceRecognitionNetで128次元の特徴ベクトルを生成
                    
                    if (detections.length > 0 && faceMatcher) {
                        const currentDescriptor = detections[0].descriptor;
                        updateDebugInfo('検出 #' + detectionCount + ': デスクリプタ長=' + currentDescriptor.length);
                        
                        // 検出したデスクリプタの最初の数値を表示（デバッグ用）
                        const currentFirstValues = Array.from(currentDescriptor.slice(0, 5)).map(v => v.toFixed(3)).join(', ');
                        console.log('Current detection descriptor sample:', currentFirstValues);
                        
                        const bestMatch = faceMatcher.findBestMatch(currentDescriptor);
                        updateDebugInfo('マッチ結果: ' + bestMatch.label + ' (距離: ' + bestMatch.distance.toFixed(3) + ')');
                        
                        // 厳格な認証: ラベルがunknownでなく、かつ距離が0.4以下の場合のみ認証成功
                        const FACE_MATCH_THRESHOLD = 0.4;
                        const isAuthenticated = bestMatch.label !== 'unknown' && bestMatch.distance <= FACE_MATCH_THRESHOLD;
                        
                        if (isAuthenticated) {
                            // 連続マッチのチェック
                            if (lastMatchedUser === bestMatch.label) {
                                consecutiveMatches++;
                            } else {
                                consecutiveMatches = 1;
                                lastMatchedUser = bestMatch.label;
                            }
                            
                            updateDebugInfo('連続マッチ: ' + consecutiveMatches + '/' + REQUIRED_CONSECUTIVE_MATCHES + ' (' + bestMatch.label + ')');
                            
                            if (consecutiveMatches >= REQUIRED_CONSECUTIVE_MATCHES) {
                                // 元のユーザー名を取得
                                const originalUsername = usernameMapping[bestMatch.label] || bestMatch.label;
                                
                                // 十分な連続マッチが確認できた場合のみログイン実行
                                lastDetectedUser = originalUsername;
                                statusMessage.className = 'status-message status-success';
                                statusMessage.textContent = '認証成功: ' + originalUsername + 'さん (距離: ' + bestMatch.distance.toFixed(3) + ', 連続: ' + consecutiveMatches + ')';
                                
                                // 手動ログインボタンを表示
                                manualLoginBtn.style.display = 'inline-block';
                                manualLoginBtn.textContent = originalUsername + 'としてログイン';
                                
                                // 自動ログインも実行（従来の動作）
                                stopCamera();
                                await performLogin(originalUsername);
                            } else {
                                // まだ十分な連続マッチが確認できていない
                                const originalUsername = usernameMapping[bestMatch.label] || bestMatch.label;
                                statusMessage.className = 'status-message status-warning';
                                statusMessage.textContent = '認証中: ' + originalUsername + 'さん (' + consecutiveMatches + '/' + REQUIRED_CONSECUTIVE_MATCHES + ')';
                                manualLoginBtn.style.display = 'none';
                            }
                            
                        } else {
                            // 認証失敗時は連続マッチをリセット
                            consecutiveMatches = 0;
                            lastMatchedUser = null;
                            lastDetectedUser = null;
                            manualLoginBtn.style.display = 'none';
                            statusMessage.className = 'status-message status-error';
                            
                            if (bestMatch.label === 'unknown') {
                                statusMessage.textContent = '未登録の顔です (距離: ' + bestMatch.distance.toFixed(3) + ')';
                                updateDebugInfo('認証失敗: 未登録の顔');
                            } else {
                                statusMessage.textContent = '顔の類似度が不十分です (距離: ' + bestMatch.distance.toFixed(3) + ' > ' + FACE_MATCH_THRESHOLD + ')';
                                updateDebugInfo('認証失敗: 閾値超過 - ' + bestMatch.label + ' (距離: ' + bestMatch.distance.toFixed(3) + ')');
                            }
                        }
                    } else {
                        if (detectionCount % 5 === 0) { // 5回に1回ログ出力
                            updateDebugInfo('検出 #' + detectionCount + ': 顔が検出されませんでした');
                        }
                    }
                } catch (error) {
                    console.error('Face detection error:', error);
                    updateDebugInfo('顔検出エラー: ' + error.message);
                    if (error.message.includes('euclideanDistance')) {
                        statusMessage.className = 'status-message status-error';
                        statusMessage.textContent = 'データ形式エラー: 顔データを再登録してください。';
                        clearInterval(detectionInterval);
                    }
                }
            }, 1000);
        }

        async function run() {
            try {
                statusMessage.textContent = 'モデルを読み込んでいます...';
                updateDebugInfo('Face-API初期化開始');
                
                await window.FaceAPIUtils.safeInitialize();
                updateDebugInfo('Face-API初期化完了');
                
                const localBaseUrl = '${pageContext.request.contextPath}/models';
                const cdnBaseUrl = 'https://cdn.jsdelivr.net/npm/face-api.js@0.22.2/weights';
                updateDebugInfo('モデル読み込み開始 - Local: ' + localBaseUrl + ', CDN: ' + cdnBaseUrl);
                
                const loadResult = await window.FaceAPIUtils.loadModelsWithFallback(localBaseUrl, cdnBaseUrl);
                
                if (!loadResult.success) {
                    throw new Error('モデルの読み込みに失敗しました: ' + loadResult.error);
                }
                
                updateDebugInfo('モデル読み込み完了: ' + loadResult.source);
                console.log('Models loaded successfully:', loadResult);
                
                statusMessage.textContent = 'カメラをセットアップしています...';
                updateDebugInfo('カメラセットアップ開始');
                
                await setupCamera();
                updateDebugInfo('カメラセットアップ完了');
                
                video.addEventListener('play', onPlay);
                video.play();
                
                statusMessage.textContent = '顔認証準備完了 - カメラに顔を向けてください';
                updateDebugInfo('顔認証システム準備完了');
                
            } catch (error) {
                console.error('Face authentication initialization failed:', error);
                statusMessage.textContent = 'エラー: ' + error.message;
                statusMessage.style.color = 'red';
                updateDebugInfo('初期化エラー: ' + error.message);
            }
        }

        // UI操作のイベントハンドラー
        toggleDebugBtn.addEventListener('click', () => {
            debugInfo.style.display = debugInfo.style.display === 'none' ? 'block' : 'none';
        });
        
        manualLoginBtn.addEventListener('click', () => {
            console.log('Manual login button clicked');
            console.log('lastDetectedUser:', lastDetectedUser);
            if (lastDetectedUser) {
                performLogin(lastDetectedUser);
            } else {
                alert('認証された顔が見つかりません。');
            }
        });
        
        restartDetectionBtn.addEventListener('click', () => {
            stopCamera();
            setTimeout(() => {
                run();
            }, 1000);
        });

        run();

        window.addEventListener('beforeunload', stopCamera);
    </script>
</body>
</html>
