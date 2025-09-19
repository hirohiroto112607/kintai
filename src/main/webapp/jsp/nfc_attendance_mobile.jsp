<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <title>NFC勤怠打刻（モバイル）</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/style.css">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <style>
        html, body {
            height: 100%;
            margin: 0;
            -webkit-user-select: none;
            -ms-user-select: none;
            user-select: none;
            -webkit-touch-callout: none;
            background-color: #f0f0f0; /* 初期灰色 */
            transition: background-color 420ms cubic-bezier(.2,.9,.2,1);
            font-family: "Hiragino Kaku Gothic ProN", "Noto Sans JP", sans-serif;
        }

        /* 全画面レイアウト（横向き想定） */
        .mobile-container {
            height: 100%;
            display: flex;
            flex-direction: row;
            align-items: center;
            justify-content: center;
            position: relative;
            overflow: hidden;
            padding: 20px;
            box-sizing: border-box;
        }

        .center-panel {
            text-align: center;
            max-width: 900px;
            width: 100%;
        }

        .title {
            font-size: 40px;
            font-weight: 700;
            margin: 8px 0 20px 0;
            color: #222;
        }

        .instruction {
            font-size: 28px;
            font-weight: 600;
            margin-bottom: 8px;
            color: #111;
        }

        .sub-instruction {
            font-size: 20px;
            color: #333;
            opacity: 0.9;
            margin-bottom: 18px;
        }

        /* 大きな状態メッセージ */
        .status-large {
            font-size: 34px;
            font-weight: 800;
            color: #fff;
            padding: 18px 28px;
            border-radius: 12px;
            display: inline-block;
            min-width: 320px;
            transition: background 320ms ease, color 320ms ease, transform 280ms cubic-bezier(.2,.9,.2,1), opacity 240ms ease;
        }

        /* 戻るボタン */
        .back-btn {
            position: absolute;
            left: 10px;
            top: 10px;
            background: rgba(255,255,255,0.9);
            color: #222;
            padding: 8px 12px;
            border-radius: 8px;
            font-weight: 700;
            text-decoration: none;
            box-shadow: 0 4px 10px rgba(0,0,0,0.12);
        }

        /* スキャン中のアニメーション（中央） */
        .spinner {
            width: 64px;
            height: 64px;
            border-radius: 50%;
            border: 8px solid rgba(255,255,255,0.2);
            border-top-color: rgba(255,255,255,0.9);
            animation: spin 1s linear infinite;
            margin: 18px auto;
        }

        @keyframes spin {
            to { transform: rotate(360deg); }
        }

        /* ポートレート時のオーバーレイ（注意表示のみ、操作をブロックしない） */
        .portrait-overlay {
            position: absolute;
            inset: 0;
            background: linear-gradient(180deg, rgba(0,0,0,0.35), rgba(0,0,0,0));
            color: #fff;
            display: none; /* デフォルトは非表示。@media で表示するが pointer-events を none にして操作をブロックしない */
            align-items: flex-start;
            justify-content: flex-start;
            text-align: left;
            padding: 18px;
            z-index: 9999;
            pointer-events: none; /* タッチを透過させる */
            opacity: 0;
            transition: opacity 420ms ease, transform 420ms cubic-bezier(.2,.9,.2,1);
        }

        .portrait-overlay .msg {
            font-size: 18px;
            font-weight: 700;
        }

        /* 小さい画面ではフォント縮小 */
        @media (max-height: 520px) {
            .title { font-size: 32px; }
            .instruction { font-size: 22px; }
            .status-large { font-size: 28px; }
            .sensor-guide { width: 120px; height: 120px; }
        }

        /* ポートレート検出（表示はするが操作はブロックしない） */
        @media (orientation: portrait) {
            .portrait-overlay { display: flex; opacity: 1; }
        }

        /* 画面左右のタッチガイド（非ブロッキング） */
        .mobile-container::before,
        .mobile-container::after {
            content: '';
            position: absolute;
            top: 0;
            bottom: 0;
            width: 36%;
            pointer-events: none;
            transition: background 240ms ease, opacity 240ms ease, transform 240ms ease;
            opacity: 0.06;
            z-index: 2;
        }
        .mobile-container::before { left: 0; background: linear-gradient(90deg, rgba(0,0,0,0.06), rgba(0,0,0,0)); }
        .mobile-container::after { right: 0; background: linear-gradient(-90deg, rgba(0,0,0,0.06), rgba(0,0,0,0)); }

        /* タッチ時のハイライト */
        body.touch-left .mobile-container::before { opacity: 0.18; transform: scaleX(1.02); }
        body.touch-right .mobile-container::after { opacity: 0.18; transform: scaleX(1.02); }

        /* 状態別の背景色（クラス制御） */
        body.result-checkin { background-color: #1976D2; }
        body.result-checkout { background-color: #28A745; }
        body.scanning { background-color: rgba(25,118,210,0.12); }

    </style>
</head>
<body>
<div class="mobile-container" id="mobileContainer" role="application" aria-label="NFC打刻画面">
    <a href="<c:url value='/attendance'/>" class="back-btn">戻る</a>

    <div class="center-panel" id="centerPanel">
        <div class="title">NFC勤怠打刻</div>
        <div class="instruction">画面をタッチして打刻開始！</div>
        <div class="sub-instruction">左側タッチ：退勤　｜　右側タッチ：出勤</div>

        <div id="statusBlock" style="margin-top:18px;">
            <div id="largeStatus" class="status-large" style="background: transparent; display: inline-block; color: #222;">
                右下をタップして開始
            </div>
            <div id="spinner" style="display:none;" class="spinner" aria-hidden="true"></div>
        </div>

        <!-- デバッグ領域（常時表示）: サーバ応答やエラーをスマホで確認できるようにここに配置 -->
        <div id="debugContainer" style="margin-top:12px; max-height:220px; overflow:auto; display:block;">
            <pre id="debugResponse" style="white-space:pre-wrap; color:#111; background:#fff; padding:10px; border-radius:8px; margin:0; display:block;">デバッグ: なし</pre>
        </div>

        <div id="resultDetail" style="margin-top:18px; color:#fff; display:none;">
            <div id="resultMessage" style="font-size:22px; font-weight:700;"></div>
            <div id="resultTime" style="margin-top:8px; font-size:16px; opacity:0.9;"></div>
        </div>
    </div>

    <!-- ポートレート時の案内（操作は可能、横向き推奨のヒント表示） -->
    <div class="portrait-overlay" id="portraitOverlay" aria-hidden="true">
        <div>
            <div class="msg">横向き推奨</div>
            <div style="margin-top:8px; font-size:14px; opacity:0.95;">横向きで右下にカードをかざすと読み取りやすくなりますが、縦画面でもタップで打刻できます。</div>
        </div>
    </div>
</div>

<script>
    // 会社用の固有識別子（既存ロジックに合わせる）
    const COMPANY_CARD_ID = "KINTAI_ATTENDANCE_CARD_2024";
    const currentUser = '<c:out value="${user.username}"/>';
    const currentRole = '<c:out value="${user.role}"/>';

    // UI 要素
    const bodyEl = document.body;
    const largeStatus = document.getElementById('largeStatus');
    const spinner = document.getElementById('spinner');
    const resultDetail = document.getElementById('resultDetail');
    const resultMessage = document.getElementById('resultMessage');
    const resultTime = document.getElementById('resultTime');
    const portraitOverlay = document.getElementById('portraitOverlay');

    // 初期背景色
    const BG_GRAY = '#f0f0f0';
    const BG_BLUE = '#1976D2';
    const BG_GREEN = '#28A745';
    const BG_ERROR = '#dc3545';

    // WebNFC 関連
    let ndefReader = null;
    let ndefController = null;
    let scanning = false;
    let lastProcessedId = null;
    let lastProcessedAt = 0;
    const DEBOUNCE_MS = 2000;

    // サポートチェック
    function checkNFCSupport() {
        if ('NDEFReader' in window) return true;
        setTemporaryError('WebNFC非対応のブラウザです。Android Chrome/Edgeで開いてください。');
        return false;
    }

    // UI 更新
    function setTemporaryError(msg, ms = 2500) {
        // 画面表示
        largeStatus.textContent = msg;
        largeStatus.style.background = BG_ERROR;
        largeStatus.style.color = '#fff';
        spinner.style.display = 'none';

        // デバッグ領域にも出す（スマホで確認しやすいように即時表示）
        try {
            const debugDiv = document.getElementById('debugResponse');
            if (debugDiv) {
                debugDiv.textContent = msg;
                debugDiv.style.display = 'block';
            }
        } catch (e) {
            console.warn('debugResponse update failed', e);
        }

        // NFC 読取中ならクリーンアップして再起動可能にする
        try {
            if (ndefController) {
                try { ndefController.abort(); } catch (e) { console.warn('Abort during setTemporaryError', e); }
            }
        } catch (e) {
            console.warn('ndefController cleanup failed in setTemporaryError', e);
        }
        ndefReader = null;
        ndefController = null;
        scanning = false;

        // 一定時間後に元の表示へ戻す
        setTimeout(() => {
            largeStatus.textContent = '右下をタップして開始';
            largeStatus.style.background = 'transparent';
            largeStatus.style.color = '#222';
            bodyEl.style.backgroundColor = BG_GRAY;
        }, ms);
    }

    function setStatusScanning() {
        spinner.style.display = 'block';
        largeStatus.style.background = 'rgba(0,0,0,0.25)';
        largeStatus.style.color = '#fff';
        largeStatus.textContent = 'スキャン中… カードを右下にかざしてください';
        // ボディに scanning クラスを付けて背景を変化させる
        try { document.body.classList.add('scanning'); } catch(e){}
    }

    function setStatusResult(action, username, timestamp) {
        spinner.style.display = 'none';
        resultDetail.style.display = 'block';

        // デバッグ表示にサーバー応答を追加
        const debugResponse = document.getElementById('debugResponse');
        debugResponse.textContent = `サーバー応答: ${action} - ${timestamp}\n対象ユーザー: ${username || currentUser}`;

        // 管理者が打刻した場合は対象ユーザー名も表示
        if (username) {
            resultMessage.textContent = (action === 'check_in' ? '出勤を記録しました: ' : '退勤を記録しました: ') + username;
        } else {
            resultMessage.textContent = action === 'check_in' ? '出勤を記録しました' : '退勤を記録しました';
        }
        resultTime.textContent = '記録時刻: ' + timestamp;

        if (action === 'check_in') {
            // 見た目は CSS クラスで切り替える
            document.body.classList.remove('scanning');
            document.body.classList.add('result-checkin');
        } else {
            document.body.classList.remove('scanning');
            document.body.classList.add('result-checkout');
        }

        // 大きなメッセージのスタイル更新
        largeStatus.textContent = resultMessage.textContent;
        largeStatus.style.background = 'rgba(0,0,0,0.25)';
        largeStatus.style.color = '#fff';

        // しばらく経ったら初期表示に戻す
        setTimeout(() => {
            // NFC リーダーがまだ動作している場合は停止してクリーンアップする
            try {
                if (ndefController) {
                    try { ndefController.abort(); } catch (e) { console.warn('Abort during cleanup', e); }
                }
            } catch (e) {
                console.warn('ndefController cleanup failed', e);
            }
            ndefReader = null;
            ndefController = null;
            scanning = false; // 再度タップで読み取りを開始できるようにフラグを戻す

            // クラスをクリアして元に戻す
            document.body.classList.remove('result-checkin', 'result-checkout', 'scanning', 'touch-left', 'touch-right');
            resultDetail.style.display = 'none';
            largeStatus.textContent = '右下をタップして開始';
            largeStatus.style.background = 'transparent';
            largeStatus.style.color = '#222';
        }, 4500);
    }

    function resetUI() {
        spinner.style.display = 'none';
        resultDetail.style.display = 'none';
        largeStatus.textContent = '右下をタップして開始';
        largeStatus.style.background = 'transparent';
        largeStatus.style.color = '#222';
        bodyEl.style.backgroundColor = BG_GRAY;
    }

    // NFC 読取の開始/停止
    async function startScanning(mode = null) {
        if (scanning) return;
        if (!checkNFCSupport()) return;

        try {
            setStatusScanning();
            ndefController = new AbortController();
            ndefReader = new NDEFReader();
            await ndefReader.scan({ signal: ndefController.signal });

            ndefReader.addEventListener('readingerror', (evt) => {
                console.error('NFC読み取りエラー', evt);
                setTemporaryError('NFC読み取りに失敗しました');
                location.reload();
            });

            ndefReader.addEventListener('reading', async (event) => {
                try {
                    const message = event.message;
                    let detectedCompanyCard = false;
                    let detectedEmployeeId = null;
                    let seenIdForDebounce = null;

                    for (const record of message.records) {
                        if (record.recordType === 'text') {
                            const textDecoder = new TextDecoder(record.encoding || 'utf-8');
                            const text = textDecoder.decode(record.data);
                            try {
                                const cardData = JSON.parse(text);
                                if (cardData.cardId === COMPANY_CARD_ID) {
                                    detectedCompanyCard = true;
                                    seenIdForDebounce = COMPANY_CARD_ID;
                                    break;
                                }
                            } catch (e) {
                                const trimmed = (text || '').trim();
                                if (trimmed.length > 0) {
                                    detectedEmployeeId = trimmed;
                                    seenIdForDebounce = trimmed;
                                    break;
                                }
                            }
                        }
                    }

                    // デバウンス
                    const now = Date.now();
                    if (seenIdForDebounce) {
                        if (lastProcessedId === seenIdForDebounce && (now - lastProcessedAt) < DEBOUNCE_MS) {
                            console.log('短時間で再読み取りされたためスキップ:', seenIdForDebounce);
                            return;
                        }
                        lastProcessedId = seenIdForDebounce;
                        lastProcessedAt = now;
                    }

                    if (detectedCompanyCard) {
                        await processAttendance(null, mode);
                        return;
                    }

                    if (detectedEmployeeId) {
                        await processAttendance(detectedEmployeeId, mode);
                        return;
                    }

                    setTemporaryError('勤怠用カードではありません');
                } catch (err) {
                    console.error('reading handler error:', err);
                    setTemporaryError('読み取り処理中にエラーが発生しました');
                }
            });

            scanning = true;
            try { document.body.classList.add('scanning'); } catch (e) {}
        } catch (err) {
            console.error('NFCスキャン開始エラー:', err);
            setTemporaryError('NFCスキャンを開始できませんでした');
            scanning = false;
            try { document.body.classList.remove('scanning'); } catch (e) {}
            ndefReader = null;
            ndefController = null;
        }
    }

    function stopScanning() {
        if (ndefController) {
            try { ndefController.abort(); } catch (e) { console.warn('Abort error', e); }
        }
        ndefReader = null;
        ndefController = null;
        scanning = false;
        resetUI();
    }

    // 勤怠処理（既存 API を利用）
    async function processAttendance(cardId, mode = null) {
        try {
            // 表示: 処理中
            setStatusScanning();

            const body = new URLSearchParams();
            if (cardId && cardId.length > 0) {
                body.append('cardId', cardId);
            } else {
                body.append('username', currentUser);
            }
            
            // モード指定がある場合は追加
            if (mode) {
                body.append('mode', mode);
            }

            const response = await fetch('${pageContext.request.contextPath}/attendance?action=nfc_attendance', {
                method: 'POST',
                credentials: 'same-origin',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: body.toString()
            });

            if (!response.ok) {
                const text = await response.text();
                console.error('勤怠処理エラー: HTTP ' + response.status, text);
                setTemporaryError('打刻に失敗しました: HTTP ' + response.status + (text ? ' - ' + text : ''));
                return;
            }

            const contentType = response.headers.get('content-type') || '';
            const responseText = await response.text();
            console.log('勤怠処理レスポンス content-type:', contentType);
            console.log('勤怠処理レスポンステキスト:', responseText);
            // デバッグ表示（スマホで確認できるように生レスポンスを画面に出す）
            try {
                const debugDiv = document.getElementById('debugResponse');
                if (debugDiv) {
                    debugDiv.textContent = responseText || '';
                    debugDiv.style.display = responseText ? 'block' : 'none';
                }
            } catch (e) {
                console.warn('debugResponse update failed', e);
            }

            if (contentType.indexOf('application/json') === -1) {
                // サーバが HTML（例: 認証画面など）を返している可能性があるため詳細表示
                console.error('予期しないレスポンス (JSON ではありません):', responseText);
                setTemporaryError('打刻に失敗しました（サーバ応答が不正）: ' + (responseText ? responseText.substring(0,200) : ''));
                return;
            }

            let data = null;
            try {
                data = JSON.parse(responseText);
            } catch (e) {
                console.error('JSON parse error:', e, responseText);
                setTemporaryError('打刻に失敗しました（JSON解析エラー）');
                return;
            }

            if (data.success) {
                setStatusResult(data.action, data.targetUsername, data.timestamp);
            } else {
                setTemporaryError('打刻に失敗しました: ' + (data.error || '不明なエラー'));
            }
        } catch (error) {
            console.error('勤怠処理エラー:', error);
            setTemporaryError('打刻処理中にエラーが発生しました');
        } finally {
            // 結果表示は setStatusResult() により一定時間表示されるため
            // stopScanning() をここで呼ばない（即時リセットを避ける）
            // 必要なら別途停止ボタンで停止できる
        }
    }

    // タッチ位置によるモード判定
    function getTouchMode(touchX, screenWidth) {
        // 画面中央を基準に右側は出勤、左側は退勤
        const mode = touchX > screenWidth / 2 ? 'check_in' : 'check_out';
        
        // デバッグ表示
        const debugResponse = document.getElementById('debugResponse');
        const modeText = mode === 'check_in' ? '出勤' : '退勤';
        debugResponse.textContent = `タッチ位置: ${touchX}px / ${screenWidth}px → ${modeText}モード`;
        
        return mode;
    }

    // タッチイベントの処理（ポートレートでも動作する）
    function handleTouchStart(event) {
        // ここではポートレートでも処理を行う。オーバーレイはヒント表示のみで pointer-events:none にしているため
        // ユーザーには画面を横向き推奨と伝えつつ、タップは受け付ける。
        try { event.preventDefault(); } catch (e) { /* ignore */ }

        const touch = (event.touches && event.touches[0]) || (event.changedTouches && event.changedTouches[0]) || event;
        const screenWidth = window.innerWidth;
        const touchX = touch.clientX || (screenWidth / 2);

        // タッチ位置でモードを決定
        const mode = getTouchMode(touchX, screenWidth);

        // 左右ハイライトを表示（短時間）
        try {
            if (mode === 'check_in') {
                document.body.classList.add('touch-right');
                document.body.classList.remove('touch-left');
            } else {
                document.body.classList.add('touch-left');
                document.body.classList.remove('touch-right');
            }
            setTimeout(() => { document.body.classList.remove('touch-left', 'touch-right'); }, 420);
        } catch (e) { console.warn('touch highlight failed', e); }

        // 現在の勤務状況を取得してモードチェック
        checkModeAndProceed(mode);
    }

    // モードチェックと処理実行
    async function checkModeAndProceed(mode) {
        try {
            // 現在の勤務状況を取得
            const response = await fetch('${pageContext.request.contextPath}/attendance?action=get_status', { 
                credentials: 'same-origin' 
            });
            
            if (!response.ok) {
                setTemporaryError('勤務状況の取得に失敗しました');
                return;
            }
            
            const data = await response.json();
            
            if (!data.success) {
                setTemporaryError('勤務状況の取得に失敗しました: ' + (data.error || '不明なエラー'));
                return;
            }
            
            const currentStatus = data.status; // "in" または "out"
            const isCurrentlyCheckedIn = currentStatus === 'in';
            
            // モードチェック
            <%-- if (mode === 'check_in' && isCurrentlyCheckedIn) {
                // 出勤中に出勤モードを選択した場合
                setTemporaryError('既に本日出勤しています。退勤モードを選択してください。');
                return;
            }
            
            if (mode === 'check_out' && !isCurrentlyCheckedIn) {
                // 退勤済みで退勤モードを選択した場合
                setTemporaryError('既に本日退勤しています。出勤モードを選択してください。');
                return;
            } --%>
            
            // モードチェック通過 - 処理を続行
            proceedWithMode(mode);
            
        } catch (error) {
            console.error('モードチェックエラー:', error);
            setTemporaryError('モードチェック中にエラーが発生しました');
        }
    }

    // モード確定後の処理
    function proceedWithMode(mode) {
        // モードに応じた表示
        if (mode === 'check_in') {
            largeStatus.textContent = '出勤モードで開始';
            largeStatus.style.background = BG_BLUE;
        } else {
            largeStatus.textContent = '退勤モードで開始';
            largeStatus.style.background = BG_GREEN;
        }
        largeStatus.style.color = '#fff';
        
        // 少し遅れてNFCスキャンを開始
        setTimeout(() => {
            startScanning(mode);
        }, 300);
    }

    // センサーガイドと画面タップのイベント
    // 右下ガイドをタップしたときに開始（自動判定モード）
    // sensorGuide.addEventListener('click', (e) => {
    //     e.stopPropagation();
    //     startScanning(); // 自動判定モード
    // });

    // 画面タッチイベント（位置によるモード指定）
    const mobileContainer = document.getElementById('mobileContainer');
    mobileContainer.addEventListener('touchstart', handleTouchStart, { passive: false });
    
    // クリックイベントも残しておく（タッチ非対応環境用）。ポートレートでもクリックで自動判定開始可能にする
    mobileContainer.addEventListener('click', (e) => {
        // 簡易フィードバック（中央タップに相当）
        try {
            document.body.classList.add('touch-right');
            setTimeout(() => { document.body.classList.remove('touch-right'); }, 320);
        } catch (e) {}
        startScanning(); // 自動判定モード
    });

    // 画面読み込み時の初期化
    document.addEventListener('DOMContentLoaded', function() {
        // 背景初期化
        bodyEl.style.backgroundColor = BG_GRAY;

        // 管理者端末の自動スキャンはモバイルでは無効にしてある（必要なら有効化可能）
        // 画面が横向きでない場合は portrait overlay が出る（CSS の @media で制御）

        // 既存のステータス取得（任意）
        try {
            getCurrentAttendanceStatus();
        } catch (e) {
            console.warn('初期ステータス取得エラー', e);
        }
    });

    // 既存の勤務状況取得（軽量版）
    async function getCurrentAttendanceStatus() {
        try {
            const response = await fetch('${pageContext.request.contextPath}/attendance?action=get_status', { credentials: 'same-origin' });
            if (!response.ok) return;
            const contentType = response.headers.get('content-type') || '';
            if (contentType.indexOf('application/json') === -1) return;
            const data = await response.json();
            // UI に簡単に反映（詳細は既存ページで実施）
            if (data && data.status) {
                // nothing major here for mobile main flow
            }
        } catch (err) {
            console.warn('勤務状況取得失敗', err);
        }
    }

</script>
</body>
</html>
