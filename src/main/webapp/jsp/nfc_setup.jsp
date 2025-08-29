<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <title>NFC設定</title>
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
        
        .button-group {
            display: flex;
            gap: 15px;
            justify-content: center;
            flex-wrap: wrap;
            margin: 20px 0;
        }
        
        @media (max-width: 600px) {
            .button-group {
                flex-direction: column;
                align-items: center;
            }
        }
    </style>
</head>
<body>
<div class="container">
    <h1>NFCカード設定</h1>
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

    <div class="nfc-instructions">
        <h3>NFCカード設定について</h3>
        <p><strong>管理者向け機能:</strong> 勤怠用NFCカードに識別子を書き込みます。</p>
        <ul>
            <li>NFCカードに会社専用の識別子を書き込みます</li>
            <li>書き込み後、そのカードが勤怠システムで使用できるようになります</li>
            <li>Android端末のChrome/Edgeブラウザでのみ動作します</li>
            <li>1枚のカードを全従業員で共用します</li>
        </ul>
        <p><strong>注意:</strong> WebNFC APIが利用できない場合は、この機能は使用できません。</p>
    </div>

    <div class="nfc-container">
        <h3>NFCカード設定</h3>
        <div id="nfcStatus" class="nfc-status" style="display: none;"></div>
        
        <div class="button-group">
            <button onclick="writeNFCCard()" class="button" id="writeButton">NFCカードに書き込み</button>
            <button onclick="readNFCCard()" class="button secondary" id="readButton">NFCカード読み取りテスト</button>
        </div>
        
        <div id="cardInfo" style="display: none; margin-top: 20px; text-align: left;">
            <h4>カード情報:</h4>
            <p id="cardContent" style="font-family: monospace; background: #f0f0f0; padding: 10px; border-radius: 3px;"></p>
        </div>
    </div>

    <div class="button-group">
        <a href="<c:url value='/attendance'/>" class="button secondary">勤怠メニューに戻る</a>
        <a href="<c:url value='/logout'/>" class="button secondary">ログアウト</a>
    </div>
</div>

<script>
// 会社用の固有識別子
const COMPANY_CARD_ID = "KINTAI_ATTENDANCE_CARD_2024";
const CARD_VERSION = "1.0";

// WebNFC APIの対応チェック
function checkNFCSupport() {
    if ('NDEFReader' in window) {
        return true;
    } else {
        showStatus('WebNFC APIはサポートされていません。Android Chrome/Edgeブラウザを使用してください。', 'error');
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

// NFCカードに書き込み
async function writeNFCCard() {
    if (!checkNFCSupport()) {
        return;
    }
    
    try {
        showStatus('NFCカードをタッチしてください...', 'warning');
        
        const ndef = new NDEFReader();
        await ndef.write({
            records: [
                {
                    recordType: "text",
                    data: JSON.stringify({
                        cardId: COMPANY_CARD_ID,
                        version: CARD_VERSION,
                        createdAt: new Date().toISOString(),
                        purpose: "attendance"
                    })
                }
            ]
        });
        
        showStatus('NFCカードへの書き込みが完了しました！', 'success');
        
    } catch (error) {
        console.error('NFC書き込みエラー:', error);
        if (error.name === 'NotAllowedError') {
            showStatus('NFC機能の使用が許可されていません。ブラウザの設定を確認してください。', 'error');
        } else if (error.name === 'NetworkError') {
            showStatus('NFCカードが検出されませんでした。カードをもう一度タッチしてください。', 'error');
        } else {
            showStatus('書き込みエラー: ' + error.message, 'error');
        }
    }
}

// NFCカード読み取りテスト
async function readNFCCard() {
    if (!checkNFCSupport()) {
        return;
    }
    
    try {
        showStatus('NFCカードをタッチしてください...', 'warning');
        
        const ndef = new NDEFReader();
        await ndef.scan();
        
        ndef.addEventListener("readingerror", () => {
            showStatus('NFCカードの読み取りに失敗しました。', 'error');
        });
        
        ndef.addEventListener("reading", ({ message, serialNumber }) => {
            console.log('NFC読み取り成功:', message);
            
            let cardData = null;
            for (const record of message.records) {
                if (record.recordType === "text") {
                    const textDecoder = new TextDecoder(record.encoding);
                    const text = textDecoder.decode(record.data);
                    try {
                        cardData = JSON.parse(text);
                        break;
                    } catch (e) {
                        // JSONでない場合はそのまま表示
                        cardData = { rawText: text };
                    }
                }
            }
            
            if (cardData) {
                if (cardData.cardId === COMPANY_CARD_ID) {
                    showStatus('正しい勤怠カードです！', 'success');
                } else {
                    showStatus('これは勤怠用カードではありません。', 'warning');
                }
                
                document.getElementById('cardContent').textContent = JSON.stringify(cardData, null, 2);
                document.getElementById('cardInfo').style.display = 'block';
            } else {
                showStatus('カードデータを読み取れませんでした。', 'error');
            }
        });
        
    } catch (error) {
        console.error('NFC読み取りエラー:', error);
        if (error.name === 'NotAllowedError') {
            showStatus('NFC機能の使用が許可されていません。ブラウザの設定を確認してください。', 'error');
        } else {
            showStatus('読み取りエラー: ' + error.message, 'error');
        }
    }
}

// ページ読み込み時の初期化
document.addEventListener('DOMContentLoaded', function() {
    // 管理者以外は書き込みボタンを無効化
    const userRole = '<c:out value="${user.role}"/>';
    if (userRole !== 'admin' && userRole !== 'ADMIN') {
        const writeButton = document.getElementById('writeButton');
        writeButton.disabled = true;
        writeButton.textContent = 'NFCカード書き込み（管理者専用）';
        writeButton.style.opacity = '0.5';
    }
    
    // WebNFCサポートチェック
    if (!checkNFCSupport()) {
        document.getElementById('writeButton').disabled = true;
        document.getElementById('readButton').disabled = true;
    }
});
</script>
</body>
</html>
