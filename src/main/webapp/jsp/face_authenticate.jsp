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
        <script src="https://cdn.jsdelivr.net/npm/@tensorflow/tfjs@4.15.0/dist/tf.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/@vladmandic/face-api@1.2.2/dist/face-api.js"></script>
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

        <div class="controls">
            <a href="${pageContext.request.contextPath}/login.jsp">通常のログインに戻る</a>
        </div>
    </div>

    <script>
        const video = document.getElementById('video');
        const statusMessage = document.getElementById('status-message');
        const labeledFaceDescriptorsJson = JSON.parse('<%= faceDescriptorsJson %>');
        let faceMatcher;
        let detectionInterval;

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

        async function onPlay() {
            if (labeledFaceDescriptorsJson.length > 0) {
                const labeledDescriptors = labeledFaceDescriptorsJson.map(ld => {
                    const descriptors = ld.descriptor.map(d => new Float32Array(Object.values(d)));
                    return new faceapi.LabeledFaceDescriptors(ld.username, descriptors);
                });
                faceMatcher = new faceapi.FaceMatcher(labeledDescriptors, 0.6);
            } else {
                statusMessage.className = 'status-message status-error';
                statusMessage.textContent = '登録されている顔がありません。';
                return;
            }

            detectionInterval = setInterval(async () => {
                const detections = await faceapi.detectAllFaces(video, new faceapi.TinyFaceDetectorOptions()).withFaceLandmarks().withFaceDescriptors();
                if (detections.length > 0 && faceMatcher) {
                    const bestMatch = faceMatcher.findBestMatch(detections[0].descriptor);
                    if (bestMatch.label !== 'unknown') {
                        statusMessage.className = 'status-message status-success';
                        statusMessage.textContent = `ようこそ、${bestMatch.label}さん！`;
                        stopCamera();
                        // Perform login
                        const formData = new FormData();
                        formData.append("username", bestMatch.label);
                        fetch("${pageContext.request.contextPath}/face/login", {
                            method: 'POST',
                            body: formData
                        }).then(response => {
                            if(response.ok) {
                                window.location.href = "${pageContext.request.contextPath}/attendance";
                            } else {
                                statusMessage.className = 'status-message status-error';
                                statusMessage.textContent = 'ログインに失敗しました。';
                            }
                        });
                    } else {
                        statusMessage.className = 'status-message status-error';
                        statusMessage.textContent = '認証に失敗しました。';
                    }
                }
            }, 1000);
        }

        async function run() {
            statusMessage.textContent = 'モデルを読み込んでいます...';
            await window.FaceAPIUtils.safeInitialize();
            const localBaseUrl = '${pageContext.request.contextPath}/models';
            const cdnBaseUrl = 'https://cdn.jsdelivr.net/npm/face-api.js@0.22.2/weights';
            await window.FaceAPIUtils.loadModelsWithFallback(localBaseUrl, cdnBaseUrl);
            statusMessage.textContent = 'カメラをセットアップしています...';
            await setupCamera();
            video.addEventListener('play', onPlay);
            video.play();
        }

        run();

        window.addEventListener('beforeunload', stopCamera);
    </script>
</body>
</html>
