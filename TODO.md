# TODO - 勤怠管理システム改善アイデア

このシステムをより面白く、使いやすくするためのアイデア一覧です。

## ゲーミフィケーション

- [ ] **バッジシステムの導入**:
  - 連続出勤日数、定時退勤率などに応じてユーザーにバッジを付与する。
  - 例：「早起き鳥バッジ」「定時退勤マスター」「パーフェクトアテンダンス」
- [ ] **ランキング機能**:
  - 部署内や全社で、特定の指標（例：朝活時間、学習時間）を競うランキングを表示する。
- [ ] **ポイントシステム**:
  - 良い行い（他の人を助ける、目標達成など）に対してポイントを付与し、景品と交換できるようにする。

## コミュニケーション活性化

- [ ] **今日のひとこと機能**:
  - 出勤時に「今日の意気込み」や「ひとこと」を登録できるようにする。
- [ ] **サンクスカード機能**:
  - 従業員同士で感謝の気持ちを伝えられるデジタルサンクスカードを送る機能。
- [ ] **リアクション機能**:
  - 他の人の勤怠状況や「ひとこと」に対して、絵文字でリアクションできるようにする。

## UI/UXの改善

- [ ] **ダッシュボードのパーソナライズ**:
  - ユーザーが自分のダッシュボードに表示する情報をカスタマイズできるようにする。
  - 勤務時間のグラフ、カレンダー、残りの有給日数などをウィジェットとして配置。
- [ ] **テーマ機能**:
  - アプリケーションの見た目を複数のテーマから選べるようにする。（ダークモードなど）
- [ ] **アバター設定**:
  - ユーザーが自分のプロフィールにアバター画像を設定できるようにする。

## 新機能

- [ ] **レポート機能の強化**:
  - 月次や年次の勤務レポートをPDFやCSVで出力できるようにする。
  - 残業時間の推移や有給消化率などをグラフで表示。
- [ ] **外部カレンダー連携**:
  - 自分の勤務スケジュールをGoogleカレンダーやOutlookカレンダーに連携できるようにする。
- [ ] **目標設定機能**:
  - 個人またはチームで週次・月次の目標を設定し、進捗を管理できるようにする。
- [x] **QRコード**:
  - 出勤・退勤時にQRコードをスキャンして打刻できる機能を追加。
  - スマートフォンアプリとの連携も検討。
  - ✅ 完了: ユーザーIDベースの自動判定QRコード機能を実装
- [ ] **NFC**:
  - NFCタグを使用して、出勤・退勤の打刻を行える機能を追加。
  - スマートフォンやNFCリーダーとの連携も検討。

## AI活用機能

- [ ] **勤務パターンの異常検知**:
  - 過去のデータから個人の標準的な勤務パターンを学習する。
  - 長時間労働や深夜労働の増加など、過重労働につながる可能性のある異常なパターンを検知し、本人や管理者にアラートを送信する。
- [ ] **打刻忘れのスマートリマインダー**:
  - 各ユーザーの平均的な勤務開始・終了時刻を学習する。
  - 打刻が行われていない場合に、AIが「打刻忘れていませんか？」と適切なタイミングでリマインドする。
- [ ] **AIアシスタント（チャットボット）**:
  - 「有給は何日残ってる？」「先月の総労働時間は？」といった自然言語での問い合わせに自動で応答するチャットボットを導入する。
  - 休暇申請や各種手続きを対話形式でサポートする。
- [ ] **生産性向上アドバイス**:
  - 勤務時間と（もし可能なら）タスクデータを分析し、個人の生産性が高い時間帯や集中を妨げている要因をフィードバックする。
  - 最適な休憩タイミングを提案する。

## 進捗サマリ

- 404対応
  - メニューからのリンクは `'/leave-requests'`。不足していたJSPを追加済み:
    - 管理者向け: leave_management.jsp
    - 従業員向け: leave_requests.jsp
- コントローラ/DAO（実処理）
  - `LeaveRequestServlet` を実装（`@WebServlet("/leave-requests")`）
    - GET: ロールに応じてビュー振り分け（管理者→`leave_management.jsp`、従業員→`leave_requests.jsp`）
    - POST: 申請（apply）/ 承認（approve）/ 却下（reject）を処理し、セッションメッセージで結果を表示
  - `LeaveRequestRepository` を実装
    - 申請追加、ユーザー別一覧、全件一覧、承認待ち一覧、承認/却下更新を提供
    - JSPで使いやすいMap形式で返却（`daysCount`/状態フラグなど）
- 競合・断片の整理
  - 以前の会話では、`DepartmentDAO` 内部に休暇申請用の内包クラス（`LeaveRequestDAO`）と、Department.java 内部に `LeaveRequest` DTO 断片が存在しているのを確認。新実装と重複の恐れあり。
- ユーザーの手動編集
  - 直近で `LeaveRequestRepository.java` と `LeaveRequestServlet.java` に手動変更が入っています（今後の編集時は必ず現状の内容を再確認してから差分適用します）。

## 現状の動作イメージ

- 従業員:
  - `leave_requests.jsp` で新規申請フォーム送信（POST `action=apply`）→ 申請履歴一覧表示。
- 管理者:
  - `leave_management.jsp` で承認待ち一覧（承認/却下の操作ボタン）、全申請履歴の参照。
- 画面間メッセージ:
  - 成功/失敗メッセージはセッション経由でページに表示されます（他画面と同じパターン）。

## 既知の懸念点・残課題

- 重複実装の排除（重要）
  - `DepartmentDAO` 内の内包 `LeaveRequestDAO`、Department.java 内の `LeaveRequest` 断片は、新しい `LeaveRequestRepository`/`LeaveRequestServlet` と役割が重複。保守性・混乱防止のため、一元化が必要です。
- leave_type のドメイン整合
  - DBスキーマ（schema.sql）の制約:
    - `leave_type IN ('paid_leave','sick_leave','special_leave','other')`
  - UIの選択肢は現在「annual/sick/personal/maternity/paternity」等を使用している箇所があるため不整合。
  - 対応案:
    - 推奨: UI→DBのマッピングをリポジトリで吸収（例: annual→paid_leave、personal→special_leave など）。新しい種別（maternity/paternity等）は `special_leave` or `other` にまとめるか、スキーマ拡張を検討。
- ステータス表記の一貫性
  - スキーマは小文字（'pending','approved','rejected'）。JSPでは状態フラグ（pending/approved/rejected）を用いる前提で整えているが、旧断片コードに大文字ステータス（"PENDING"など）が残っている可能性あり。新実装に統一したい。
- IDE/依存解決の警告
  - 「Jakarta import 解決不能」的なIDE警告が一部で言及あり。pom.xml の Jakarta Servlet/JSP/JSTL 依存やプラグインの同期確認が必要（ビルドは通っていれば実害は少ないが、IDE同期で解消可能）。

## 直近のおすすめ対応（安全な順）

1) 重複の整理（スリム化）

- `DepartmentDAO` 内の内包 `LeaveRequestDAO` と Department.java 内の `LeaveRequest` 断片を撤去 or 非参照化し、`LeaveRequestRepository` に一元化。
- 既存コードから旧DAO/DTOを参照していないことを確認（参照があれば新Repositoryに置換）。

2) leave_type の整合ポリシー確定

- UI→DBマッピングを `LeaveRequestRepository.addLeaveRequest` などで明示（annual→paid_leave, sick→sick_leave, personal→special_leave, maternity/paternity→special_leave or other 等）。
- JSP表示側は見出しは現状の日本語ラベルで問題なし。保存値のみマッピング。

3) ステータスの小文字統一

- 読み出し時に小文字で統一し、JSPが利用する boolean フラグ（pending/approved/rejected）を確実にセット。

4) 依存/解析の安定化

- pom.xml の Jakarta/JSTL 依存を再同期（Eclipse/Mavenのプロジェクト更新）。
- ビルドとデプロイのパスを確認（Jetty/Tomcatなど README 手順に沿って）。

## 軽い動作確認ポイント

- GET `/leave-requests`
  - 管理者: 承認待ち一覧＋全件一覧が表示されること
  - 従業員: 自身の申請一覧＋新規申請フォームが表示されること
- POST `/leave-requests`
  - `action=apply`: 妥当な日付範囲で申請が追加され、一覧に反映
  - `action=approve/reject`: 管理者で承認/却下でき、状態/承認者/処理日が更新
- メッセージ表示: 成功/エラーが意図通り出ること

## 補足（現在開いているファイル）

- `leave_management.jsp` は管理者向け画面です。以下のリクエスト属性を前提にしています:
  - `pendingRequests`, `allRequests`, `successMessage`, `errorMessage`
- 直近で `LeaveRequestRepository.java` と `LeaveRequestServlet.java` に手動変更が入っているため、次の編集を行う際はその差分を取り込みつつ一元化を進めます。

## 次の一手（提案）

- 方針の確認をお願いします（短い確認でOK）
  - 旧 `LeaveRequestDAO`（`DepartmentDAO` 内）と Department.java 内の `LeaveRequest` 断片は削除/無効化して、`LeaveRequestRepository` に統一してよいか
  - leave_type のマッピング（annual/personal/maternity/paternity の扱い）を上記提案で進めてよいか（必要なら具体的なマッピング表をこちらで反映）
