/**
 * Face-API初期化スクリプト
 * TensorFlow.jsとface-api.jsの安全な初期化管理
 */

// グローバル初期化状態管理
window.FaceAPIState = window.FaceAPIState || {
    tensorflowReady: false,
    faceapiReady: false,
    modelsLoaded: false,
    initPromise: null,
    errorCount: 0,
    lastError: null
};

/**
 * 一度だけ実行される安全な初期化
 */
async function safeInitialize() {
    // 既に初期化中または完了している場合は待機/返却
    if (window.FaceAPIState.initPromise) {
        return window.FaceAPIState.initPromise;
    }

    // 初期化プロミスを作成
    window.FaceAPIState.initPromise = (async () => {
        try {
            console.log('Starting safe Face-API initialization...');

            // ライブラリの存在確認
            if (typeof tf === 'undefined') {
                throw new Error('TensorFlow.js is not loaded');
            }
            if (typeof faceapi === 'undefined') {
                throw new Error('face-api.js is not loaded');
            }

            // TensorFlow.jsの安全な初期化
            if (!window.FaceAPIState.tensorflowReady) {
                console.log('Initializing TensorFlow.js...');
                
                // より安全なバックエンド初期化
                try {
                    await tf.ready();
                    const currentBackend = tf.getBackend();
                    console.log('Current TensorFlow backend:', currentBackend);
                    
                    // バックエンドが正常に機能するかテスト
                    const testTensor = tf.tensor1d([1, 2, 3]);
                    const testResult = testTensor.sum();
                    await testResult.data(); // データの取得をテスト
                    testTensor.dispose();
                    testResult.dispose();
                    
                    console.log('TensorFlow.js backend test passed');
                } catch (e) {
                    console.error('TensorFlow.js backend test failed:', e);
                    window.FaceAPIState.lastError = e;
                    throw new Error('TensorFlow.js backend initialization failed: ' + e.message);
                }
                
                window.FaceAPIState.tensorflowReady = true;
                console.log('TensorFlow.js ready');
            }

            // face-api.jsの準備確認
            if (!window.FaceAPIState.faceapiReady) {
                console.log('Verifying face-api.js...');
                
                // face-api.jsの主要なオブジェクトが利用可能か確認
                if (!faceapi.nets || !faceapi.detectAllFaces) {
                    throw new Error('face-api.js is not properly loaded');
                }
                
                // face-api.jsの基本機能テスト
                try {
                    const TinyFaceDetectorOptions = faceapi.TinyFaceDetectorOptions;
                    if (!TinyFaceDetectorOptions) {
                        throw new Error('TinyFaceDetectorOptions not available');
                    }
                    console.log('face-api.js function test passed');
                } catch (e) {
                    console.error('face-api.js function test failed:', e);
                    window.FaceAPIState.lastError = e;
                    throw new Error('face-api.js function test failed: ' + e.message);
                }
                
                window.FaceAPIState.faceapiReady = true;
                console.log('face-api.js ready');
            }

            // エラーカウントをリセット
            window.FaceAPIState.errorCount = 0;
            window.FaceAPIState.lastError = null;

            return {
                success: true,
                tensorflowReady: window.FaceAPIState.tensorflowReady,
                faceapiReady: window.FaceAPIState.faceapiReady
            };

        } catch (error) {
            console.error('Face-API initialization failed:', error);
            window.FaceAPIState.errorCount++;
            window.FaceAPIState.lastError = error;
            
            // 失敗時はプロミスをリセット
            window.FaceAPIState.initPromise = null;
            throw error;
        }
    })();

    return window.FaceAPIState.initPromise;
}

/**
 * モデル読み込み（改良版）
 */
async function loadFaceAPIModels(baseUrl, retryCount = 0) {
    const maxRetries = 2;
    
    const modelsToLoad = [
        {
            name: 'tinyFaceDetector',
            path: 'tiny_face_detector/',
            loader: () => faceapi.nets.tinyFaceDetector.loadFromUri(baseUrl + 'tiny_face_detector/')
        },
        {
            name: 'faceLandmark68Net', 
            path: 'face_landmark_68/',
            loader: () => faceapi.nets.faceLandmark68Net.loadFromUri(baseUrl + 'face_landmark_68/')
        },
        {
            name: 'faceRecognitionNet',
            path: 'face_recognition/',
            loader: () => faceapi.nets.faceRecognitionNet.loadFromUri(baseUrl + 'face_recognition/')
        }
    ];

    const results = [];
    for (const model of modelsToLoad) {
        try {
            console.log(`Loading ${model.name} from ${baseUrl + model.path}...`);
            await model.loader();
            console.log(`✓ Successfully loaded ${model.name}`);
            results.push({ name: model.name, success: true });
        } catch (error) {
            console.error(`✗ Failed to load ${model.name}:`, error.message);
            results.push({ 
                name: model.name, 
                success: false, 
                error: error.message,
                fullUrl: baseUrl + model.path
            });
        }
    }

    return results;
}

/**
 * モデル読み込み（フォールバック付き）
 */
async function loadModelsWithFallback(localBaseUrl, cdnBaseUrl) {
    // 初期化状態を確認
    if (window.FaceAPIState.modelsLoaded) {
        console.log('Models already loaded');
        return { success: true, source: 'cached' };
    }

    try {
        // 安全な初期化を確認
        await safeInitialize();

        console.log('=== Starting model loading ===');
        console.log('Local URL:', localBaseUrl);
        console.log('CDN URL:', cdnBaseUrl);

        // まずローカルから試行
        console.log('Attempting to load models from local...');
        let localResults = await loadFaceAPIModels(localBaseUrl);
        
        // すべて成功した場合
        const allLocalSuccess = localResults.every(r => r.success);
        if (allLocalSuccess) {
            console.log('✓ All models loaded successfully from local');
            window.FaceAPIState.modelsLoaded = true;
            return { success: true, source: 'local', results: localResults };
        }

        // 失敗したモデルがあればCDNから再試行
        const failedModels = localResults.filter(r => !r.success);
        console.log(`Failed models (${failedModels.length}):`, failedModels.map(r => r.name));
        
        console.log('Attempting to load failed models from CDN...');
        const cdnResults = await loadFaceAPIModels(cdnBaseUrl);
        
        // 結果をマージ
        const finalResults = [...localResults];
        for (const cdnResult of cdnResults) {
            const localIndex = finalResults.findIndex(r => r.name === cdnResult.name);
            if (localIndex >= 0 && !finalResults[localIndex].success && cdnResult.success) {
                finalResults[localIndex] = { ...cdnResult, source: 'cdn' };
            }
        }

        // 最終的な成功確認
        const allFinalSuccess = finalResults.every(r => r.success);
        if (allFinalSuccess) {
            console.log('✓ All models loaded successfully (mixed sources)');
            window.FaceAPIState.modelsLoaded = true;
            return { success: true, source: 'mixed', results: finalResults };
        } else {
            const stillFailed = finalResults.filter(r => !r.success);
            throw new Error(`Failed to load models: ${stillFailed.map(r => r.name).join(', ')}`);
        }

    } catch (error) {
        console.error('Model loading failed:', error);
        return { 
            success: false, 
            error: error.message,
            details: error
        };
    }
}

// エクスポート
window.FaceAPIUtils = {
    safeInitialize,
    loadModelsWithFallback,
    getState: () => window.FaceAPIState
};