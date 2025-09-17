# NFC モバイル画面：作業メモとTODO

## 要約（短縮）

- 目的：横画面（ランドスケープ）専用のモバイル向け「社員証NFC読み取り画面」を新規追加し、右下のセンサー位置ガイド／タッチでスキャン開始／背景色で出退勤表示／デバッグ表示を実装した。
- 実装ファイル：`src/main/webapp/jsp/nfc_attendance_mobile.jsp`
- 主な不具合報告：読み取り後すぐに初期画面へ戻る、画面をタッチしてから数秒でNFCが反応しなくなる。
- 対応：processAttendance の即時 stop を削除、結果表示後に安全に abort するクリーンアップを追加、DEBOUNCE_MS を 2000ms に修正、デバッグ出力領域を追加。

## 変更ファイル（主要）

- 追加：`src/main/webapp/jsp/nfc_attendance_mobile.jsp`
- 更新：`src/main/webapp/jsp/nfc_attendance.jsp`（モバイル画面へのリンク追加）
- 確認対象（サーバ側）：`src/main/java/com/example/attendance/controller/AttendanceServlet.java`（POST action=nfc_attendance の挙動確認）

## 主要関数・定数（フロントエンド）

- startScanning()
  - NDEFReader を生成、`scan({ signal: ndefController.signal })` を実行。
  - 'reading' イベントで NDEF メッセージを解析し、社員ID または COMPANY_CARD_ID を判定。
  - デバウンス管理（lastProcessedId / lastProcessedAt, DEBOUNCE_MS = 2000）。
- processAttendance(cardId)
  - fetch('${pageContext.request.contextPath}/attendance?action=nfc_attendance', { method: 'POST', credentials: 'same-origin', body: URLSearchParams })
  - レスポンスを text() で受け取り content-type をチェック → JSON.parse → setStatusResult を呼ぶ。
  - 変更: 最初は finally で stopScanning() を即時呼んでいたが、これを削除（直ちに初期画面に戻る不具合の原因）。
- setStatusResult(action, targetUsername, timestamp)
  - BG_BLUE / BG_GREEN に背景色切替、結果表示のための timeout 後に ndefController.abort() と scanning = false でクリーンアップする処理を追加。
- setTemporaryError(msg)
  - エラーメッセージ表示 + debugResponse に出力。abort() と scanning = false のクリーンアップを行う。

## 発見した不具合と原因推定

- 読取後すぐに最初の画面に戻る
  - 原因：processAttendance の finally で即時 stopScanning() を呼んでいたため（UIが結果表示前にリセット）。
- 画面をタッチしてから何秒かでNFCが反応しなくなる
  - 原因候補：
    - フロント側の AbortController が意図せず abort されている、または参照が残っている。
    - ブラウザや端末（WebNFC 実装）側でスキャンセッションが時間経過で中断される仕様やバグ。
    - サーバ側でリダイレクトや HTML を返しており、フロントが異常終了している可能性。

## 実施した修正（履歴）

- processAttendance の finally での stopScanning 呼び出しを削除（即時リセットを停止）。
- setStatusResult の timeout 内で安全に ndefController.abort()/ndefReader=null/scanning=false を行うクリーンアップを追加（結果表示が終わった後で停止）。
- setTemporaryError でエラー時に abort() と scanning フラグ戻しを追加。
- fetch のレスポンスを一旦 text() で取得し、content-type をチェックして raw レスポンスを debug 領域に出力するように変更。
- DEBOUNCE_MS を誤設定（2）から 2000 に修正。
- `nfc_attendance.jsp` にモバイル画面へのリンクを追加。
- 画面上に `<pre id="debugResponse">` を追加してスマホ単体でレスポンスを確認できるようにした。

## 残タスク（優先順）

- [ ] docs/TODO.md にテスト手順と注意点を追加（本ファイル：このタスクで追記中）
- [ ] Android 実機（Chrome / Edge）での動作確認（再現条件取得）
- [ ] ユーザーからの現地デバッグ情報取得（debugResponse の出力内容・スクショ・反応が止まるまでの秒数）
- [ ] サーバ側（AttendanceServlet）に一時的なデバッグログを追加して、POST 到達確認（ユーザー許可要）
- [ ] UI に「停止ボタン」を追加して手動で scan を停止できるようにする案の実装と検証
- [ ] ndefController の参照管理を更に堅牢化（多重 abort を防ぐガード）
- [ ] 端末依存の挙動を調査（ブラウザベンダーや OS の WebNFC 実装依存か確認）

## 次の実行ステップ（提案・要確認）

- ユーザーに確認が必要なこと（必須）
  1. サーバ側に一時ログを入れてよいか（console ログ / サーバ標準出力に受信時刻・ユーザー・body を出力）。→ ユーザー承認が必要（ログの副作用は小さいが運用ポリシー確認）。
  2. Android 実機での試験をユーザーが行えるか（実機で debugResponse の出力をコピー／スクショして共有してほしい）。
- 私が行う提案アクション（ユーザー承認後に実行）
  - AttendanceServlet の nfc_attendance ハンドラに受信ログを追加してデバッグ情報を収集。
  - UI に明示的な「停止ボタン」を追加して、タイミング依存の自動停止を防ぐ代替ハンドリングを試作。
- すぐに実行可能な簡易手順（ユーザーが行う）
  1. モバイルで `/jsp/nfc_attendance_mobile.jsp` を開く。
  2. 画面右下をタップしてスキャンを開始、カードをかざす。
  3. 反応が止まるまでの秒数を計測して報告。
  4. 画面の「デバッグ:」以下のテキストをコピーして送る、あるいはスクリーンショットを送る。

## 追加の実装・修正候補

以下は現状の実装や既知の不具合、将来的な機能拡張を踏まえた "他に実装すべき機能・修正すべき点" の候補一覧です。
優先度、概算工数、担当候補、影響範囲、実施時の注意点を付記しています。必要に応じてチケット化してください。

### 優先度: 高

- NFC モバイル画面: 明示的な「停止」ボタンの追加
  - 概要: 自動で停止してしまう問題の回避用にユーザーが手動で停止できるコントロールを追加する。
  - 影響範囲: `nfc_attendance_mobile.jsp` の UI と `startScanning()`/`stopScanning()` ロジック
  - 概算: 0.5 - 1 日
  - 担当候補: フロントエンド担当
  - 注意: AbortController の多重 abort を防ぐガードを入れる（冪等化）

- サーバ側デバッグログ（任意フラグ）
  - 概要: POST `/attendance?action=nfc_attendance` 到達を確認するための一時ログ出力（受信時刻、body、ユーザー agent、処理結果）
  - 影響範囲: `AttendanceServlet.java`（テスト・検証用のログのみ、本番では無効化）
  - 概算: 1 - 2 時間
  - 担当候補: サーバ担当
  - 注意: 個人情報取り扱いポリシーに従い、ログ出力はユーザー了承のもとで有効化する

### 優先度: 中

- ndefController の参照・状態管理の堅牢化
  - 概要: AbortController の状態遷移を明確化し、多重参照や古いコントローラ参照で abort される問題を防止する。
  - 実装案: コントローラをオブジェクトでラップし、状態列挙（idle/scanning/aborting）を持たせる。abort() 呼び出しは状態をチェックして冪等化。
  - 概算: 1 - 2 日
  - 影響範囲: `nfc_attendance_mobile.jsp` と `nfc_attendance.jsp` の共通ロジック

- DEBOUNCE ロジックの強化とログ出力
  - 概要: 現在のデバウンス（DEBOUNCE_MS=2000）は暫定。連続読み取りケースや誤読時の扱いを明確化するため、成功時/失敗時で異なるデバウンスやホワイトリスト/ブラックリストを検討。
  - 概算: 0.5 - 1 日

### 優先度: 低

- ブラウザ互換性テスト/ワークアラウンド実装
  - 概要: WebNFC の実装差分で反応が途切れる可能性があるため、主要ブラウザ（Chrome/Edge/Firefox for Android）での確認と機種別の既知の挙動ドキュメント化を行う。
  - 概算: 1 - 3 日（実機依存）

- UI/UX 改善: 読取中インジケータとアクセシビリティ対応
  - 概要: スキャン中の視覚的なフィードバック（アニメーション、読み取りガイド）やスクリーンリーダー向けの ARIA ラベルを追加する。
  - 概算: 0.5 - 1 日

### 優先度: 将来的/拡張機能

- 再試行ポリシーとオフライン対策
  - 概要: ネットワーク失敗時に一定回数のリトライやキューイングを行い、オフライン時の一時打刻をバッファして再送する仕組みを検討。
  - 影響範囲: フロントエンド（IndexedDB/localStorage）とサーバ側 API の idempotency 設計
  - 概算: 2 - 5 日

- セキュリティ: CSRF と認証の再確認
  - 概要: NFC 経由の打刻リクエストに対する CSRF トークンの検証、セッション/トークン寿命の確認。既に JWT 実装がある場合はモバイル画面からの認証ヘッダ対応を確認。
  - 概算: 0.5 - 2 日

### テストケース（必ず用意する）

- モバイル（Android）での連続スキャン（1分間に10回以上）で正常にデバウンスが効くこと
- スキャン後に結果表示が消えてから再スキャンが可能になること（UIフロー）
- ネットワークエラー発生時の挙動（エラーメッセージ、再試行、abort の挙動）
- サーバが HTML を返す・リダイレクトする場合のフロントの安全なハンドリング

### ドキュメント化/運用

- 運用手順: デバッグログを一時有効化する際の手順とオフに戻す手順をドキュメント化する
- テスト手順: `docs/TODO.md` に記載されている簡易手順を `docs/DATABASE_SETUP.md` や `docs/TASKS.md` の該当箇所へリンク追加

## 連絡用メモ

- 端末依存のためログと実機情報が重要。まずはユーザー確認のうえサーバログ追加→実機で再現→ログ解析、が最短ルート。

## Androidアプリクラッシュ修正

### 要約

- 問題：LeaveRequestActivity.javaの149行目でクラッシュ発生（IllegalArgumentException）
- 原因：submitLeaveRequestメソッドでemployeeIdやreasonがnullの場合、FormBody.Builder().add()が例外を投げる
- 修正：パラメータのnullチェックを追加し、null時はエラーメッセージを表示して処理中断

## 実施した修正

- [x] LeaveRequestActivity.javaのsubmitLeaveRequestメソッド冒頭にnullチェック追加
- [x] nullの場合、UIをリセットしてエラーメッセージ表示
- [x] MainActivity.javaのleaveButtonクリックリスナーでemployeeIdをIntentにputExtra
- [x] SharedPreferencesから保存されたusernameをemployeeIdとして渡す
- [x] SERVER_URLにコンテキストパス"/kintai"を追加（404エラー修正）
- [x] デバッグログを追加してパラメータ値をログ出力
- [x] コンパイルエラー多数発生（依存関係未設定）だが、修正自体は有効

## 残タスク

- [ ] Androidプロジェクトの依存関係設定（OkHttp, Android SDK）
- [ ] 修正後のアプリビルドとテスト
- [ ] 実機でのクラッシュ再現確認

## 顔認証・QRコード同時打刻画面実装

### 要約

- 目的：顔認証とQRコード読み取りを同時に扱える新しい勤怠打刻画面を作成
- 仕様：1つのカメラで顔認証（自動打刻）とQRコード検知（手動打刻）を並行処理
- 技術：face-api.js + html5-qrcode.js統合、JavaScript Promiseベースの並行実行

## 実装タスク

- [x] FaceAttendanceServlet.java 作成（打刻API）
- [x] face_qr_attendance.jsp 作成（統合UI）
- [x] employee_nav.jsp / admin_nav.jsp にリンク追加
- [x] JavaScriptで顔認証とQRコードの同時処理実装
- [x] レスポンシブデザイン対応

## 機能仕様

- **顔認証**: カメラで顔検知 → 連続3回マッチで自動打刻
- **QRコード**: カメラでQR検知 → 手動「打刻実行」ボタン表示
- **UI**: 左右レイアウト（カメラ/結果表示）、リアルタイムステータス更新
- **エラー処理**: 各機能独立したエラーハンドリング、デバッグ情報表示

## 修正履歴（2025/09/16）

### 認証中ユーザー名表示問題修正

- **問題**: 顔認証中に「さん (/)」と表示され、誰の認証なのかわからない
- **原因**: FaceDataDAO.java で username が null/空文字の場合の処理が不十分、JSP で originalUsername の null チェックが不完全
- **修正内容**:
  - FaceDataDAO.java: `getAllFaceDescriptors()` で username が null/空文字の場合はスキップする処理を追加
  - face_qr_attendance.jsp: `usernameMapping` 構築時に null/undefined チェックを強化、フォールバック処理を追加
  - デバッグ情報を強化して問題特定を容易に
- **結果**: 認証中に正しいユーザー名が表示されるよう修正

## テスト・検証

- [x] ビルド確認（mvn clean compile）
- [x] データベース確認（不正データなし）
- [ ] ブラウザ互換性テスト（Chrome/Edge/Firefox）
- [ ] カメラ権限とマイク権限の処理確認
- [ ] 顔認証精度テスト（登録済みユーザーでのマッチング）
- [ ] QRコード読み取りテスト（各種QRコード）
- [ ] モバイルデバイスでの動作確認
- [ ] 打刻処理の正確性確認（出勤/退勤の自動判定）

## 技術詳細

- カメラストリーム: 640x480, facingMode: 'user'
- 顔認証閾値: 0.4, 連続マッチ: 3回
- QR検知間隔: 500ms
- デバウンス: 3秒（重複スキャン防止）

## トークンベース認証実装

### 要約

- 目的：サーバー（Javaサーブレット）とアプリ（Android）の両方でJWTベースのトークン認証を実装
- 仕様：JWTトークン、有効期限1年、リフレッシュトークンなし、既存セッション認証を置き換え
- 技術：サーバー側java-jwtライブラリ使用

## 実装タスク（サーバー側）

- [x] pom.xmlにJWTライブラリ依存関係追加
- [x] TokenUtilクラス作成（トークン生成・検証ユーティリティ）
- [x] LoginServlet修正（JSONレスポンスでトークン返却）
- [x] AuthenticationFilter修正（Authorizationヘッダー検証）
- [x] 全サーブレットのセッション依存をトークン検証に変更（AttendanceServlet完了）
- [x] API_SPEC.md更新（トークン認証仕様反映）
- [x] プロジェクトのコンパイル確認

## 実装タスク（アプリ側）

- [ ] MainActivityにログイン画面追加（ユーザー名/パスワード入力）
- [ ] ApiClientクラス作成（HTTPリクエスト共通処理）
- [ ] 認証API呼び出し実装（POST /login）
- [ ] SharedPreferencesでトークン保存・管理
- [ ] 全アクティビティでAuthorizationヘッダー付与
- [ ] トークン有効期限チェックと再ログイン処理
- [ ] パスキー対応

## テスト・検証

- [ ] サーバー側単体テスト（トークン生成・検証）
- [ ] アプリ側ログイン機能テスト
- [ ] API通信テスト（トークン付与）
- [ ] エラー処理テスト（無効トークン、期限切れ）
- [ ] 既存機能の動作確認（セッション依存除去後）

## 技術詳細

- JWTペイロード：username, role, exp
- 署名：HS256、固定秘密鍵
- エラーレスポンス：401 Unauthorized（無効トークン）
- アプリ保存：SharedPreferences（暗号化考慮）
