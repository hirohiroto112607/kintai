# API仕様書

このドキュメントは本アプリケーションで提供される主要なHTTPエンドポイントの仕様をまとめたものです。各エンドポイントについて、HTTPメソッド、リクエストパラメータ、認証要件、レスポンス形式、主なステータスコード、例を記載します。

## 共通事項

- セッションベースの認証を利用します。ログイン後に `HttpSession` に `user`（User DTO）が格納されます。多くのエンドポイントはログインが必須です。
- 管理者機能は `user.getRole()` が `"admin"` の場合に利用可能です。
- HTML画面は JSP を返し、AJAX/API は JSON を返します。
- 日付は基本的に `YYYY-MM-DD`、日時は `yyyy-MM-dd HH:mm:ss` などのフォーマットを使用します。

---

## /login

- メソッド: GET, POST
- 説明:
  - GET: ログインページを返す（`/login.jsp` にフォワード）。
  - POST: フォーム認証（`username`, `password`）を行い、成功時は `/attendance` にリダイレクト。
- リクエスト:
  - POST (form)
    - username: string (必須)
    - password: string (必須)
- 認証: 不要
- レスポンス:
  - 成功: 302 リダイレクト `/attendance`
  - 失敗: login.jsp をフォワードし、`errorMessage` を表示
- 例 (curl 相当):

```bash
POST /login
form: username=alice, password=secret
```

---

## /logout

- メソッド: GET
- 説明: セッションを無効化して `/login.jsp` にリダイレクト
- 認証: 任意（セッションがあれば破棄）
- レスポンス: 302 リダイレクト `/login.jsp`

---

## /attendance  (および /attendance/status)

- メソッド: GET, POST
- 主な用途: 勤怠表示、出退勤、CSVエクスポート、NFC/手動編集など

### GET /attendance

- クエリパラメータ (主なもの)
  - action: `test` | `get_status` | `export_csv` | ...
  - filterUserId, startDate, endDate (管理者向けフィルタ)
- 認証: 必須
- 振る舞い:
  - 一般ユーザー: `/jsp/employee_menu.jsp` を返す
  - 管理者: `/jsp/admin_menu.jsp` を返す（フィルタや統計を含む）
  - `action=test`: テスト用HTMLを返す
  - `action=export_csv` & role=admin: CSV を添付で返す (`text/csv`, Content-Disposition)
  - 認証されていないAJAXリクエストには JSON で 401 を返す:

```json
{
  "success": false,
  "error": "認証が必要です"
}
```

### GET /attendance/status  または /attendance?action=get_status

- 説明: 現在の出退勤ステータスを JSON で返す
- 認証: 必須
- レスポンス (成功 200):

```json
{
  "success": true,
  "status": "in" | "out",
  "lastActivity": "出勤: HH:mm" | "退勤: HH:mm" | "本日の記録なし"
}
```

- エラー: 認証未取得時や内部エラーは success: false を返す

### POST /attendance

- フォームパラメータ:
  - action: `check_in` | `check_out` | `nfc_attendance` | `add_manual` | `delete_manual`
  - check_in/check_out: action 値のみで処理
  - add_manual/delete_manual: userId, checkInTime (ISO), checkOutTime (ISO optional)
  - nfc_attendance: cardId (管理者端末からの代打刻), mode (optional: `check_in`/`check_out`)
- 認証: 必須
- レスポンス: 操作後に `/attendance` へリダイレクト（画面経由の処理）
- 権限:
  - add_manual/delete_manual は `admin` のみ

---

## /departments

- メソッド: GET, POST
- 説明: 部署管理画面・操作
- 認証: 管理者 (`admin`) 必須
- GET:
  - query: action=edit, departmentId 等（編集対象を返す）
  - レスポンス: `/jsp/department_management.jsp`
- POST (form):
  - action = `add` | `update` | `delete`
  - add/update:
    - departmentId (string)
    - departmentName (string)
    - description (string)
    - enabled (`on` for true)
  - delete:
    - departmentId
- 成功時はセッションに successMessage をセットして `/departments` にリダイレクト

---

## /users

- メソッド: GET, POST
- 説明: ユーザー管理（管理者のみ）
- 認証: 管理者 (`admin`) 必須
- GET:
  - action=edit&username=... で編集対象を渡し `/jsp/user_management.jsp` を返す
- POST (form):
  - action = `add` | `update` | `delete` | `reset_password` | `toggle_enabled`
  - add:
    - username, password, role, departmentId (nullable)
  - update:
    - username, role, departmentId, enabled (`true`), password (optional)
  - delete/reset_password/toggle_enabled:
    - username
- パスワードはサーバ側でハッシュ化して保存（UserDAO.hashPassword）

---

## /leave-requests

- メソッド: GET, POST
- 説明: 休暇申請の参照／申請／承認／却下
- 認証: 必須（承認/却下は管理者のみ）
- GET:
  - 管理者: pendingRequests / allRequests を `/jsp/leave_management.jsp` に設定
  - 一般ユーザー: 自分の申請を `/jsp/leave_requests.jsp` に設定
- POST (form):
  - action = `apply` | `approve` | `reject`
  - apply:
    - leaveType, startDate, endDate, reason
  - approve:
    - requestId
  - reject:
    - requestId, rejectionReason (or promptReason fallback)
- 成功時はセッションに successMessage をセットしてリダイレクト

---

## /qr

- メソッド: GET, POST
- 説明: QRコード生成、QRスキャンによる打刻（AJAX/画面）
- 認証: 必須
- GET:
  - view=scanner -> `/jsp/qr_scanner.jsp`
  - action=user_id -> 現在ユーザーの userId を埋めた QR 画像を JSON で返す
    - レスポンス (成功):

```json
{
  "success": true,
  "qrCodeImage": "data:image/png;base64,<...>",
  "userId": "alice",
  "type": "user_id"
}
```

- userId query がある場合 -> processUserIdScan (通常画面向け、jsp にフォワード)
- POST:
  - action=user_id_scan (AJAX)
    - form: userId (required)
    - 管理者は任意の userId を処理可能（他者の打刻）、
      一般ユーザーは自身の userId のみ許可
    - レスポンス (成功):

```json
{
  "success": true,
  "message": "出勤が記録されました。今日も頑張りましょう！",
  "action": "checkin"
}
```

    - エラー時は success: false + error メッセージを返す

---

## Passkey (WebAuthn) エンドポイント

- 全体: WebAuthn のオプション生成や完了処理を行う。主に JSON を介してやり取り。

### GET /passkey/authenticate/start

- 説明: 認証開始用の PublicKeyCredentialRequestOptions を返す
- クエリ:
  - username (オプション): 指定されたユーザーの認証器に限定
- 認証: 任意（クライアントが認証器を使ってログインするための開始）
- レスポンス: JSON (WebAuthn options)

### POST /passkey/authenticate/finish

- 説明: クライアントから返された credentialId 等を受け取り、登録済みの認証器を照合してログインを成立させる（簡易実装、完全なWebAuthn検証はライブラリ依存）
- リクエスト:
  - JSON body または form param:
    - credentialId または rawId 等（base64url や hex 等、様々な形式を試行して照合）
- 成功:

```json
{ "success": true, "redirectUrl": "/attendance" }
```

- 失敗:
  - 400: 認証器が見つからない等
  - 500: サーバ内部エラー

### GET /passkey/register/start

- 説明: 登録開始用の PublicKeyCredentialCreationOptions を返す
- 必要: ログイン済みユーザー（セッションに `user` または `loggedInUser` が存在）
- セッションに challenge と userId を保存
- レスポンス: JSON (WebAuthn 作成オプション)

### POST /passkey/register/finish

- 説明: クライアントから送られた attestation のレスポンスを受け、Authenticator 情報をDBに保存する（実運用では検証を厳密に行う必要あり）
- リクエスト:
  - JSON body (WebAuthn の response オブジェクトを含む)
  - server は `session.challenge` と `session.userId` を参照
- 処理:
  - clientDataJSON / attestationObject / rawId を受け取り、必要に応じてデコード・パースして Authenticator を保存
- 成功: 登録後に `/jsp/employee_menu.jsp` へリダイレクト
- 失敗: 500 や DB エラーを JSON で返す

---

## エラーハンドリング / ステータスコード（代表）

- 200: 正常（JSON API の成功）
- 302: 処理後のリダイレクト（画面遷移）
- 400: リクエスト不備（必須パラメータ欠落等）
- 401: 認証が必要（セッション無し等）
- 403: 権限がない（管理者専用を非管理者がアクセス）
- 500: サーバ内部エラー、DBエラー等

---

## 例: 出退勤ステータス取得 (fetch)

```bash
GET /attendance?action=get_status
Accept: application/json
Cookie: JSESSIONID=...
```

レスポンス:

```json
{
  "success": true,
  "status": "in",
  "lastActivity": "出勤: 09:03"
}
```

---

## 注意事項 / 今後の改善点

- Passkey 関連はライブラリと実装の差異があるため、実運用では WebAuthn の完全検証（クライアントデータと attestation の検証、リプレイ対策、signCount の扱い等）を導入すること。
- JSON API のエラーレスポンス形式を統一するとクライアント実装が容易になります（例: { success: boolean, error?: string, data?: any }）。
- 日付・時刻のバリデーションとタイムゾーン考慮（UTC で保管する等）を検討すること。
