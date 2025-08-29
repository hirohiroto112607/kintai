# TODO - タスク

- [x] Passkey 認証の照合ロジックを多形式(base64url / base64 / hex / raw)に対応させる
- [x] セッション互換: 認証後に "user" と "loggedInUser" を両方設定し /attendance にリダイレクトする
- [x] login.jsp に `credentials: 'same-origin'` を追加し、/passkey_register.jsp へのリンクを実装
- [x] AuthenticatorDAO.save を最小カラム挿入に簡略化して INSERT エラーを低減
- [ ] 本番用にデバッグ出力を Logger（SLF4J 等）に置き換え、ログレベルを整理する
- [ ] DB スキーマ整合性の確保：マイグレーションで必須カラムにデフォルトを設定するか、DAO.save で必須列を確実に埋める処理を追加する
- [ ] Passkey の統合テスト（E2E）を作成して登録→認証→セッション→画面表示までを自動化する
- [ ] AuthenticatorDAO の検索・保存周りにユニットテストを追加（バイナリ照合・encode/encode fallback の検証）
- [ ] 不要な例外スタックトレースの整理とユーザー向けエラーメッセージの統一
- [ ] PROGRESS.md に今回の修正概要（ファイル / 変更点 / 検証結果）を追記する
- [ ] ステージング環境での負荷/並列認証検証（sign_count の競合確認）

変更済みファイル（今回の修正）
- src/main/java/com/example/attendance/controller/PasskeyAuthenticationFinishServlet.java
- src/main/java/com/example/attendance/dao/AuthenticatorDAO.java
- src/main/webapp/login.jsp

短い検証メモ
- 登録時に DB に credential が挿入され、認証時に該当 Authenticator を発見してセッションが設定されることを手動確認済み。
- employee_menu.jsp の表示（${user.username} 等）と出勤処理で user が null にならないことを確認済み。

次の作業を指定してください（例）:
- ログ整備（System.out → SLF4J）をこちらで実装してほしい
- DAO.save を public_key など追加カラムに安全に値を入れるようさらに改修してほしい
- E2E テストを作成して CI に組み込んでほしい
