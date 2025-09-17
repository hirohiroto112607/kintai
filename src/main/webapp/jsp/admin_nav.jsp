<%-- admin_nav.jsp --%>
<%@page contentType="text/html" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<style>
/* ナビ全体 */
.admin-nav {
  display: flex;
  flex-wrap: wrap;       /* 画面が狭ければ折り返す */
  gap: 12px;             /* ボタン間の余白を少し増やす */
  align-items: center;
  padding: 16px 20px;    /* パディングを増やして余裕を持たせる */
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%); /* よりソフトなグラデーション背景 */
  border-radius: 12px;   /* 全体を丸く */
  box-shadow: 0 4px 12px rgba(0,0,0,0.1); /* 軽いシャドウ */
  margin: 10px 0;        /* 上下マージン */
}

/* 共通ボタン */
.admin-nav .button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 16px;    /* パディングを増やしてタッチしやすく */
  height: 44px;          /* 高さを少し増やす */
  min-width: 120px;      /* 幅を広げる */
  max-width: 260px;      /* 最大幅も調整 */
  white-space: nowrap;   /* テキストの改行を防ぐ */
  overflow: hidden;
  text-overflow: ellipsis;
  text-decoration: none;
  font-size: 15px;       /* フォントサイズを少し大きく */
  font-weight: 500;      /* 太字に */
  line-height: 1.2;
  border-radius: 8px;    /* 角を丸く */
  box-sizing: border-box;
  border: none;          /* ボーダーを削除 */
  background: #f8f9fa;   /* ソフトなグレー背景 */
  color: #333;           /* ダークグレー文字 */
  box-shadow: 0 2px 6px rgba(0,0,0,0.1); /* ボタンにシャドウ */
  transition: all 0.3s ease; /* スムーズなトランジション */
  cursor: pointer;
}

/* ホバー効果 */
.admin-nav .button:hover {
  background: #e9ecef;   /* ホバーでより明るく */
  box-shadow: 0 4px 12px rgba(0,0,0,0.15); /* シャドウ強調 */
  transform: translateY(-2px); /* 少し浮かせる */
}

/* セカンダリーボタン */
.admin-nav .button.secondary {
  background: #dee2e6;
  color: #6c757d;
}

/* セカンダリーホバー */
.admin-nav .button.secondary:hover {
  background: #ced4da;
}

/* 各ボタンにカラフルな色を割り当て */
.admin-nav input[value="出勤"] {
  background: linear-gradient(135deg, #4caf50 0%, #388e3c 100%); /* 緑 */
  color: #fff;
}
.admin-nav input[value="出勤"]:hover {
  background: linear-gradient(135deg, #388e3c 0%, #2e7d32 100%);
}

.admin-nav input[value="退勤"] {
  background: linear-gradient(135deg, #f44336 0%, #d32f2f 100%); /* 赤 */
  color: #fff;
}
.admin-nav input[value="退勤"]:hover {
  background: linear-gradient(135deg, #d32f2f 0%, #b71c1c 100%);
}

.admin-nav a[href*="attendance"] {
  background: linear-gradient(135deg, #2196f3 0%, #1976d2 100%); /* 青 */
  color: #fff;
}
.admin-nav a[href*="attendance"]:hover {
  background: linear-gradient(135deg, #1976d2 0%, #1565c0 100%);
}

.admin-nav a[href*="users"] {
  background: linear-gradient(135deg, #9c27b0 0%, #7b1fa2 100%); /* 紫 */
  color: #fff;
}
.admin-nav a[href*="users"]:hover {
  background: linear-gradient(135deg, #7b1fa2 0%, #6a1b9a 100%);
}

.admin-nav a[href*="departments"] {
  background: linear-gradient(135deg, #ff9800 0%, #f57c00 100%); /* オレンジ */
  color: #fff;
}
.admin-nav a[href*="departments"]:hover {
  background: linear-gradient(135deg, #f57c00 0%, #ef6c00 100%);
}

.admin-nav a[href*="leave-requests"] {
  background: linear-gradient(135deg, #e91e63 0%, #c2185b 100%); /* ピンク */
  color: #fff;
}
.admin-nav a[href*="leave-requests"]:hover {
  background: linear-gradient(135deg, #c2185b 0%, #ad1457 100%);
}

.admin-nav a[href*="qr"] {
  background: linear-gradient(135deg, #4caf50 0%, #388e3c 100%); /* 緑 */
  color: #fff;
}
.admin-nav a[href*="qr"]:hover {
  background: linear-gradient(135deg, #388e3c 0%, #2e7d32 100%);
}

.admin-nav a[href*="face/attendance"] {
  background: linear-gradient(135deg, #ff9800 0%, #f57c00 100%); /* オレンジ */
  color: #fff;
}
.admin-nav a[href*="face/attendance"]:hover {
  background: linear-gradient(135deg, #f57c00 0%, #ef6c00 100%);
}

.admin-nav a[href*="nfc_attendance"] {
  background: linear-gradient(135deg, #9c27b0 0%, #7b1fa2 100%); /* 紫 */
  color: #fff;
}
.admin-nav a[href*="nfc_attendance"]:hover {
  background: linear-gradient(135deg, #7b1fa2 0%, #6a1b9a 100%);
}

.admin-nav a[href*="nfc_setup"] {
  background: linear-gradient(135deg, #e91e63 0%, #c2185b 100%); /* ピンク */
  color: #fff;
}
.admin-nav a[href*="nfc_setup"]:hover {
  background: linear-gradient(135deg, #c2185b 0%, #ad1457 100%);
}

.admin-nav a[href*="face/manage"] {
  background: linear-gradient(135deg, #2196f3 0%, #1976d2 100%); /* 青 */
  color: #fff;
}
.admin-nav a[href*="face/manage"]:hover {
  background: linear-gradient(135deg, #1976d2 0%, #1565c0 100%);
}

.admin-nav a[href*="face/authenticate"] {
  background: linear-gradient(135deg, #f44336 0%, #d32f2f 100%); /* 赤 */
  color: #fff;
}
.admin-nav a[href*="face/authenticate"]:hover {
  background: linear-gradient(135deg, #d32f2f 0%, #b71c1c 100%);
}

.admin-nav a[href*="passkey_register"] {
  /* デフォルトのまま */
}

.admin-nav a[href*="logout"] {
  /* セカンダリーのまま */
}

/* フォーカス状態（アクセシビリティ） */
.admin-nav .button:focus {
  outline: 2px solid #007bff;
  outline-offset: 2px;
}

/* 小さい画面向け */
@media (max-width: 480px) {
  .admin-nav {
    padding: 12px 16px;
    gap: 8px;
  }
  .admin-nav .button {
    padding: 8px 12px;
    height: 40px;
    min-width: 100px;
    font-size: 14px;
  }
}
</style>

<nav class="admin-nav">
        <form action="<c:url value='/attendance'/>" method="post" style="display:inline;">
            <input type="hidden" name="action" value="check_in">
            <input type="submit" value="出勤" class="button">
        </form>
        <form action="<c:url value='/attendance'/>" method="post" style="display:inline;">
            <input type="hidden" name="action" value="check_out">
            <input type="submit" value="退勤" class="button">
        </form>
  <a href="<c:url value='/attendance'/>" class="button">勤怠履歴管理</a>
  <a href="<c:url value='/users'/>" class="button">ユーザー管理</a>
  <a href="<c:url value='/departments'/>" class="button">部署管理</a>
  <a href="<c:url value='/leave-requests'/>" class="button" style="background-color: #17a2b8;">休暇申請管理</a>
  <a href="<c:url value='/qr'/>" class="button" style="background-color: #28a745;">QRコード打刻</a>
  <a href="<c:url value='/face/attendance'/>" class="button" style="background-color: #fd7e14;">顔・QR打刻</a>
  <a href="<c:url value='/jsp/nfc_attendance.jsp'/>" class="button" style="background-color: #6f42c1;">NFC勤怠打刻</a>
  <a href="<c:url value='/jsp/nfc_setup.jsp'/>" class="button" style="background-color: #fd7e14;">NFC設定</a>
  <a href="<c:url value='/face/manage'/>" class="button" style="background-color: #e83e8c;">顔データ管理</a>
  <a href="<c:url value='/face/authenticate'/>" class="button" style="background-color: #dc3545;">顔認証テスト</a>
  <a href="<c:url value='/passkey_register.jsp'/>" class="button">パスキーを登録</a>
  <a href="<c:url value='/logout'/>" class="button secondary">ログアウト</a>
</nav>
