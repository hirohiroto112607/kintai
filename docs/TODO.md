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

## 連絡用メモ
- 端末依存のためログと実機情報が重要。まずはユーザー確認のうえサーバログ追加→実機で再現→ログ解析、が最短ルート。

# トークンベース認証実装

## 要約
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
