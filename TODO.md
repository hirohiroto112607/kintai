# TODO - Passkey / WebAuthn 実装タスク

- [x] TODO リスト作成
- [ ] 要件の再確認（このファイルと既存ログを参照）
  - Java11, Jakarta Servlet, Maven, webauthn4j, PostgreSQL
- [ ] mvn clean compile を実行してコンパイルエラーを洗い出す
- [ ] 見つかったコンパイルエラーを順に修正する
  - Jakarta API の import 不整合を確認
  - webauthn4j の依存とバージョン整合性確認
- [ ] DB マイグレーション適用（開発 DB）
  1. db_migrations/001_add_attested_credential_data.sql を適用
  2. db_migrations/002_add_public_key_column.sql を適用
- [ ] サーバ再起動（Jetty）してログを監視
  - mvn -Djetty.port=8081 jetty:run
- [ ] ブラウザで登録フローを実行して Network / Server ログを収集
  - POST /kintai/passkey/register/finish の Request Payload とサーバ STDOUT の DEBUG 行
- [ ] passkey_register.jsp のクライアント側コード確認・修正
  - base64url helper の正確性（RFC4648 URL-safe）
  - options.authenticatorSelection フィールド名整合（サーバ側の出力を参照）
  - rawId を payload に含める実装確認
- [ ] PasskeyRegistrationFinishServlet の入出力処理を安定化
  - raw body バイト列処理、UTF-8 サニタイズ、base64url decode
  - CollectedClientData / AttestationObject のパース回復処理
  - attestedCredentialData が無い場合の最小保存フォールバック
- [ ] AuthenticatorDAO と DB 保存フォーマットを確定
  - credential_id, attested_credential_data, public_key, sign_count の整合性
- [ ] webauthn4j を使った厳密な検証を実装（後段）
  - RegistrationData / RegistrationParameters による検証
  - ServerProperty の生成（origin, rpId, challenge, tokenBinding）
- [ ] PasskeyAuthenticationFinishServlet の署名検証実装
  - Assertion の検証、signCount の更新ロジック
- [ ] E2E 手順作成・手動または自動テスト
  - 登録 → ログアウト → パスキーでログイン を検証
- [ ] ドキュメント更新（README.md / PROGRESS.md）
- [ ] リリース前のコード整備と不要デバッグログ削除

メモ:
- 当面は「最小限の保存」で動作確認を優先し、webauthn4j による厳密検証は後続で実装する。
- ブラウザは localhost を開発用セキュアコンテキストとして扱う。
