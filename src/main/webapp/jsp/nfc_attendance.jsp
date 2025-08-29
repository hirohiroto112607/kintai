<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <title>NFC勤怠打刻</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/style.css">
    <style>
        .nfc-container {
            text-align: center;
            margin: 30px 0;
            padding: 20px;
            border: 2px dashed #ddd;
            border-radius: 10px;
            background-color: #f9f9f9;
        }
        
        .nfc-status {
            margin: 20px 0;
            padding: 15px;
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
        
        .status-warning {
            background-color: #fff3cd;
            color: #856404;
            border: 1px solid #ffeaa7;
        }
        
        .status-info {
            background-color: #cce7ff;
            color: #004085;
            border: 1px solid #8cc8ff;
        }
        
        .nfc-instructions {
            background-color: #e3f2fd;
            border: 1px solid #2196f3;
            padding: 15px;
            border-radius: 5px;
            margin: 20px 0;
            text-align: left;
        }
        
        .nfc-instructions h3 {
            margin-top: 0;
            color: #1976d2;
        }
        
        .attendance-status {
            background-color: #f8f9fa;
            border: 1px solid #dee2e6;
            padding: 15px;
            border-radius: 5px;
            margin: 20px 0;
        }
        
        .status-in {
            color: #28a745;
            font-weight: bold;
        }
        
        .status-out {
            color: #dc3545;
            font-weight: bold;
        }
        
        .nfc-action-button {
            font-size: 1.2em;
            padding: 15px 30px;
            margin: 20px 0;
            border-radius: 8px;
            min-width: 200px;
        }
        
        .button-group {
            display: flex;
            gap: 15px;
            justify-content: center;
            flex-wrap: wrap;
            margin: 20px 0;
        }
        
        .loading-spinner {
            display: inline-block;
            width: 20px;
            height: 20px;
            border: 3px solid rgba(255,255,255,.3);
            border-radius: 50%;
            border-top-color: #fff;
            animation: spin 1s ease-in-out infinite;
        }
        
        @keyframes spin {
            to { transform: rotate(360deg); }
        }
        
        @media (max-width: 600px) {
            .button-group {
                flex-direction: column;
                align-items: center;
            }
            
            .nfc-action-button {
                min-width: auto;
                width: 100%;
            }
        }
    </style>
</head>
<body>
<div class="container">
    <h1>NFC勤怠打刻</h1>
    <p>ようこそ, <c:out value="${user.username}"/>さん</p>
    
    <c:choose>
        <c:when test="${user.role == 'admin' or user.role == 'ADMIN'}">
            <jsp:include page="admin_nav.jsp" flush="true" />
        </c:when>
        <c:when test="${user.role == 'employee' or user.role == 'EMPLOYEE'}">
            <jsp:include page="employee_nav.jsp" flush="true" />
        </c:when>
        <c:otherwise>
            <jsp:include page="employee_nav.jsp" flush="true" />
        </c:otherwise>
    </c:choose>

    <c:if test="${not empty successMessage}">
        <p class="success-message"><c:out value="${successMessage}"/></p>
    </c:if>
    
    <c:if test="${not empty errorMessage}">
        <p class="error-message"><c:out value="${errorMessage}"/></p>
    </c:if>

    <div class="attendance-status">
        <h3>現在の勤務状況</h3>
        <div id="currentStatus">
            <span id="statusText">状況を確認中...</span>
        </div>
        <div id="lastActivity" style="margin-top: 10px; font-size: 0.9em; color: #666;">
        </div>
    </div>

    <div class="nfc-instructions">
        <h3>NFC勤怠打刻の使用方法</h3>
        <ul>
            <li><strong>1枚の共用カード:</strong> 会社で用意されたNFCカードを使用します</li>
            <li><strong>自動切り替え:</strong> カードをタッチするたびに出勤・退勤が自動で切り替わります</li>
            <li><strong>対応デバイス:</strong> Android端末のChrome/Edgeブラウザでのみ動作します</li>
            <li><strong>共存システム:</strong> QRコードシステムと併用できます</li>
        </ul>
        <p><strong>注意:</strong> WebNFC APIが利用できない場合は、QRコードシステムをご利用ください。</p>
    </div>

    <div class="nfc-container">
        <h3>NFC勤怠打刻</h3>
        <div id="nfcStatus" class="nfc-status" style="display: none;"></div>
        
        <button onclick="toggleNFCAttendance()" class="button nfc-action-button" id="nfcButton">
            NFCカードで打刻
        </button>
        
        <div id="attendanceResult" style="display: none; margin-top: 20px;">
            <h4>打刻結果:</h4>
            <p id="resultMessage" style="font-weight: bold;"></p>
            <p id="resultTime" style="font-size: 0.9em; color: #666;"></p>
        </div>
    </div>

    <div class="button-group">
        <a href="<c:url value='/jsp/nfc_setup.jsp'/>" class="button secondary">NFC設定</a>
        <a href="<c:url value='/jsp/nfc_attendance_mobile.jsp'/>" class="button secondary">モバイル打刻画面</a>
        <a href="<c:url value='/jsp/qr_menu.jsp'/>" class="button secondary">QRコード打刻</a>
        <a href="<c:url value='/attendance'/>" class="button secondary">勤怠メニューに戻る</a>
        <a href="<c:url value='/logout'/>" class="button secondary">ログアウト</a>
    </div>
</div>

<script>
 // 会社用の固有識別子
 const COMPANY_CARD_ID = "KINTAI_ATTENDANCE_CARD_2024"; // 共有カード用識別子（既存の挙動を維持）
 // 管理者端末上にログインしているユーザー（端末のセッション）
 const currentUser = '<c:out value="${user.username}"/>';
 const currentRole = '<c:out value="${user.role}"/>';
 
 // ここでは「カードに書かれたテキスト」が DB の users.username と一致する想定です。
 // 共有カード（JSON 内に cardId が COMPANY_CARD_ID）と社員証（plain text の username）の両方を扱います。

// WebNFC APIの対応チェック
function checkNFCSupport() {
    if ('NDEFReader' in window) {
        return true;
    } else {
        showStatus('WebNFC APIはサポートされていません。Android Chrome/Edgeブラウザを使用するか、QRコードシステムをご利用ください。', 'error');
        return false;
    }
}

// ステータス表示
function showStatus(message, type) {
    const statusDiv = document.getElementById('nfcStatus');
    statusDiv.style.display = 'block';
    statusDiv.textContent = message;
    statusDiv.className = 'nfc-status status-' + type;
}

 // 現在の勤務状況を取得
async function getCurrentAttendanceStatus() {
    try {
        // セッション Cookie を確実に送るため credentials を指定
        const response = await fetch('${pageContext.request.contextPath}/attendance?action=get_status', { credentials: 'same-origin' });
        
        if (!response.ok) {
            console.error('勤務状況取得エラー: HTTP ' + response.status);
            document.getElementById('statusText').textContent = '状況の取得に失敗しました';
            return;
        }
        
        const contentType = response.headers.get('content-type') || '';
        if (contentType.indexOf('application/json') === -1) {
            // サーバーが HTML を返している（例: 認証切れでログイン画面を返す等）
            const text = await response.text();
            console.error('予期しないレスポンス (JSON ではありません):', text);
            document.getElementById('statusText').textContent = '状況の取得に失敗しました';
            return;
        }
        
        const data = await response.json();
        
        if (data.success) {
            updateStatusDisplay(data.status, data.lastActivity);
        } else {
            document.getElementById('statusText').textContent = '状況の取得に失敗しました';
        }
    } catch (error) {
        console.error('勤務状況取得エラー:', error);
        document.getElementById('statusText').textContent = '状況の取得に失敗しました';
    }
}

// 勤務状況表示の更新
function updateStatusDisplay(status, lastActivity) {
    const statusText = document.getElementById('statusText');
    const lastActivityDiv = document.getElementById('lastActivity');
    
    if (status === 'in') {
        statusText.innerHTML = '<span class="status-in">出勤中</span>';
        statusText.appendChild(document.createTextNode(' - 次回は退勤になります'));
    } else {
        statusText.innerHTML = '<span class="status-out">退勤済み</span>';
        statusText.appendChild(document.createTextNode(' - 次回は出勤になります'));
    }
    
    if (lastActivity) {
        lastActivityDiv.textContent = '最終活動: ' + lastActivity;
    }
}

// NFC勤怠打刻開始
let ndefReader = null;
let ndefController = null;
let scanning = false;
let lastProcessedId = null;
let lastProcessedAt = 0;
const DEBOUNCE_MS = 2000; // 同一カードの連続読み取りを防ぐ間隔(ms)

function toggleNFCAttendance() {
    if (scanning) {
        stopScanning();
    } else {
        startScanning();
    }
}

async function startScanning() {
    if (!checkNFCSupport()) return;

    const button = document.getElementById('nfcButton');
    button.disabled = false;
    button.innerHTML = '停止';
    showStatus('NFCスキャンを開始しました。カードをかざしてください...', 'info');

    try {
        ndefController = new AbortController();
        ndefReader = new NDEFReader();
        await ndefReader.scan({ signal: ndefController.signal });

        ndefReader.addEventListener('readingerror', (evt) => {
            console.error('NFC読み取りエラー', evt);
            showStatus('NFCカードの読み取りに失敗しました。', 'error');
        });

        ndefReader.addEventListener('reading', async (event) => {
            try {
                const message = event.message;
                console.log('NFC読み取り:', message);

                // 抽出処理（共有カード(JSON) or 社員証(plain text username)）
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

                // デバウンス: 同一カードの連続処理を回避
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
                    // 共有カードフロー: 現在ログイン中のユーザーで打刻
                    showStatus('共有カード検出: 管理者端末ログインユーザーで打刻します...', 'info');
                    await processAttendance();
                    return;
                }

                if (detectedEmployeeId) {
                    // 社員証フロー: 指定ユーザーを管理者端末から打刻
                    showStatus('社員証を検出しました: ' + detectedEmployeeId + ' — 打刻処理中...', 'info');
                    await processAttendance(detectedEmployeeId);
                    return;
                }

                showStatus('これは勤怠用カードではありません。正しいカードを使用してください。', 'error');

            } catch (err) {
                console.error('reading handler error:', err);
                showStatus('読み取り処理中にエラーが発生しました: ' + err.message, 'error');
            }
        });

        scanning = true;
    } catch (err) {
        console.error('NFCスキャン開始エラー:', err);
        showStatus('NFCスキャンを開始できませんでした: ' + (err.message || err), 'error');
        // ボタンを元に戻す
        document.getElementById('nfcButton').textContent = 'NFCカードで打刻';
        scanning = false;
        ndefReader = null;
        ndefController = null;
    }
}

function stopScanning() {
    if (ndefController) {
        try {
            ndefController.abort();
        } catch (e) {
            console.warn('Abort error', e);
        }
    }
    ndefReader = null;
    ndefController = null;
    scanning = false;
    document.getElementById('nfcButton').textContent = 'NFCカードで打刻';
    showStatus('NFCスキャンを停止しました', 'warning');
}

 // 勤怠打刻処理
 // 引数 cardId を渡すと管理者端末からそのユーザーを打刻する（社員証フロー）。
 // 引数を渡さない場合は従来の端末ログインユーザーで打刻（共有カードや個人端末想定）。
async function processAttendance(cardId) {
    try {
        showStatus('勤怠を記録中...', 'info');
        
        // セッションを送るため credentials を追加、ボディは URLSearchParams を使用
        const body = new URLSearchParams();
        if (cardId && cardId.length > 0) {
            body.append('cardId', cardId);
        } else {
            body.append('username', currentUser);
        }
        
        const response = await fetch('${pageContext.request.contextPath}/attendance?action=nfc_attendance', {
            method: 'POST',
            credentials: 'same-origin',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: body.toString()
        });
        
        if (!response.ok) {
            console.error('勤怠処理エラー: HTTP ' + response.status);
            showStatus('打刻に失敗しました: HTTP ' + response.status, 'error');
            return;
        }
        
        const contentType = response.headers.get('content-type') || '';
        if (contentType.indexOf('application/json') === -1) {
            const text = await response.text();
            console.error('予期しないレスポンス (JSON ではありません):', text);
            showStatus('打刻に失敗しました', 'error');
            return;
        }
        
        const data = await response.json();
        
        if (data.success) {
            showStatus('打刻完了！', 'success');
            
            // 結果表示
            const resultDiv = document.getElementById('attendanceResult');
            const resultMessage = document.getElementById('resultMessage');
            const resultTime = document.getElementById('resultTime');
            
            // 管理者が打刻した場合は対象ユーザー名も表示
            if (data.targetUsername) {
                resultMessage.textContent = (data.action === 'check_in' ? '出勤を記録しました: ' : '退勤を記録しました: ') + data.targetUsername;
            } else {
                resultMessage.textContent = data.action === 'check_in' ? '出勤を記録しました' : '退勤を記録しました';
            }
            resultTime.textContent = '記録時刻: ' + data.timestamp;
            resultDiv.style.display = 'block';
            
            // 勤務状況を更新
            setTimeout(() => {
                getCurrentAttendanceStatus();
            }, 1000);
            
        } else {
            showStatus('打刻に失敗しました: ' + (data.error || '不明なエラー'), 'error');
        }
    } catch (error) {
        console.error('勤怠処理エラー:', error);
        showStatus('打刻処理中にエラーが発生しました: ' + error.message, 'error');
    }
}

// ボタンをリセット
function resetButton(button, originalText) {
    button.disabled = false;
    button.textContent = originalText;
}

 // ページ読み込み時の初期化
document.addEventListener('DOMContentLoaded', function() {
    // WebNFCサポートチェック
    if (!checkNFCSupport()) {
        document.getElementById('nfcButton').disabled = true;
    } else {
        // 管理者端末でログイン中なら自動で継続スキャンを開始
        try {
            if (currentRole && String(currentRole).toLowerCase() === 'admin') {
                startScanning();
            }
        } catch (e) {
            console.warn('自動スキャン開始に失敗しました:', e);
        }
    }
    
    // 現在の勤務状況を取得
    getCurrentAttendanceStatus();
});
</script>
</body>
</html>
