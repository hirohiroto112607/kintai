<%-- admin_nav.jsp --%>
<%@page contentType="text/html" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<style>
/* ナビ全体 */
.employee-nav {
  display: flex;
  flex-wrap: wrap;       /* 画面が狭ければ折り返す */
  gap: 8px;              /* ボタン間の余白 */
  align-items: center;
  padding: 8px 0;
}

/* 共通ボタン */
.employee-nav .button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6px 12px;
  height: 36px;          /* 高さを抑える */
  min-width: 100px;      /* 小さすぎない幅 */
  max-width: 240px;      /* 横に伸びすぎない */
  white-space: nowrap;   /* テキストの改行を防ぐ */
  overflow: hidden;
  text-overflow: ellipsis;
  text-decoration: none;
  font-size: 14px;
  line-height: 1;
  border-radius: 6px;
  box-sizing: border-box;
  border: 1px solid rgba(0,0,0,0.08);
  background: #f0f0f0;
  color: #222;
}

/* セカンダリーボタン */
.employee-nav .button.secondary {
  background: #f8f9fa;
}

/* インライン style で色指定している要素を上書きしたい場合（必要なら） */
.employee-nav .button[style*="background-color: #17a2b8"] {
  background: #17a2b8;
  color: #fff;
}
.employee-nav .button[style*="background-color: #28a745"] {
  background: #28a745;
  color: #fff;
}

/* 小さい画面向け */
@media (max-width: 480px) {
  .employee-nav .button {
    padding: 6px 10px;
    height: 32px;
    min-width: 80px;
    font-size: 13px;
  }
}
</style>

<nav class="employee-nav">
        <form action="<c:url value='/attendance'/>" method="post" style="display:inline;">
            <input type="hidden" name="action" value="check_in">
            <input type="submit" value="出勤" class="button">
        </form>
        <form action="<c:url value='/attendance'/>" method="post" style="display:inline;">
            <input type="hidden" name="action" value="check_out">
            <input type="submit" value="退勤" class="button">
        </form>
        <a href="<c:url value='/qr'/>" class="button" style="background-color: #28a745;">QRコード打刻</a>
        <a href="<c:url value='/jsp/nfc_attendance.jsp'/>" class="button" style="background-color: #6f42c1;">NFC勤怠打刻</a>
        <a href="<c:url value='/leave-requests'/>" class="button" style="background-color: #17a2b8;">休暇申請</a>
        <a href="<c:url value='/passkey_register.jsp'/>" class="button">パスキーを登録</a>
</nav>
