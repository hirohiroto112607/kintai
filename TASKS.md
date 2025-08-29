# パスキー導入タスク

## 概要
WebAuthn (パスキー) を利用したパスワードレス認証を導入する。
ライブラリには `webauthn4j` を使用する。

## 実装計画

1.  **ライブラリの追加:** (Done)
    - `pom.xml` に `webauthn4j-core` と `webauthn4j-util` の依存関係を追加する。

2.  **データベースの変更:** (Done)
    - `schema.sql`: 公開鍵クレデンシャル情報を保存するための `authenticators` テーブルを追加する。
    - 対応するDTOを作成する。

3.  **バックエンドの実装 (Java):** (Done)
    - **登録処理:**
        - `PasskeyRegistrationServlet` を新規作成する。
        - チャレンジを生成し、フロントエンドからのデータを検証・保存するロジックを実装する。
    - **認証処理:**
        - `LoginServlet` を変更または新規作成する。
        - チャレンジを生成し、フロントエンドからの認証データを検証するロジックを実装する。
    - **DAOの変更:**
        - `authenticators` テーブルへのCRUD操作を行うメソッドをDAOに追加する。

4.  **フロントエンドの実装 (JSP/JavaScript):** (Done)
    - `login.jsp` やユーザー管理画面にパスキー登録・認証のUIを追加する。
    - JavaScriptで `navigator.credentials.create()` と `navigator.credentials.get()` を使用し、バックエンドと通信する処理を実装する。
