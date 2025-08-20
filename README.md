# 勤怠管理システム

Java EE（Jakarta EE）を使用した勤怠管理システムです。従業員の出勤・退勤の記録と管理者による勤怠データの管理機能を提供します。

## 🚀 クイックスタート

```bash
# 1. 依存関係の解決とコンパイル
mvn clean compile

# 2. Jettyサーバーで実行
mvn jetty:run

# 3. ブラウザでアクセス
http://localhost:8080/kintai
```

## 機能概要

### 👤 従業員機能

- ✅ 出勤・退勤の記録
- 📊 自分の勤怠履歴の閲覧
- 🔐 セキュアなログイン・ログアウト

### 👨‍💼 管理者機能

- 📈 全従業員の勤怠履歴の閲覧・管理
- 🔍 勤怠データのフィルタリング（ユーザー、期間）
- ✏️ 勤怠記録の手動追加・削除
- 📄 CSVエクスポート機能
- 👥 ユーザー管理（追加、編集、削除、パスワードリセット）

## 🛠️ 技術スタック

- **Java**: 17+
- **Jakarta EE**: Servlet API 6.0
- **JSP**: ビューテンプレート
- **JSTL**: JSPタグライブラリ  
- **PostgreSQL**: データベース
- **HikariCP**: コネクションプール
- **CSS**: レスポンシブなUI
- **Maven**: ビルドツール
- **Jetty**: 開発用サーバー

## 📁 プロジェクト構成

```text
kintai/
├── src/main/java/com/example/attendance/
│   ├── dto/                       # データ転送オブジェクト
│   │   ├── Attendance.java        # 勤怠情報
│   │   └── User.java              # ユーザー情報
│   ├── dao/                       # データアクセスオブジェクト
│   │   ├── AttendanceDAO.java     # 勤怠データ操作（PostgreSQL対応）
│   │   └── UserDAO.java           # ユーザーデータ操作（PostgreSQL対応）
│   ├── controller/                # サーブレット（コントローラー）
│   │   ├── LoginServlet.java      # ログイン処理
│   │   ├── LogoutServlet.java     # ログアウト処理
│   │   ├── AttendanceServlet.java # 勤怠管理
│   │   └── UserServlet.java       # ユーザー管理
│   ├── filter/                    # フィルター
│   │   └── AuthenticationFilter.java # 認証フィルター
│   ├── util/                      # ユーティリティ
│   │   └── DatabaseUtil.java     # データベース接続管理
│   └── listener/                  # リスナー
│       └── DatabaseContextListener.java # DB接続プール管理
├── src/main/webapp/
│   ├── jsp/                       # JSPファイル
│   │   ├── admin_menu.jsp         # 管理者画面
│   │   ├── employee_menu.jsp      # 従業員画面
│   │   ├── error.jsp              # エラー画面
│   │   └── user_management.jsp    # ユーザー管理画面
│   ├── WEB-INF/
│   │   └── web.xml                # Web設定ファイル
│   ├── login.jsp                  # ログイン画面
│   └── style.css                  # スタイルシート
├── src/main/resources/
│   └── schema.sql                 # データベーススキーマ
├── pom.xml                        # Maven設定
├── README.md                      # このファイル
└── DATABASE_SETUP.md              # データベースセットアップガイド
```

## ⚙️ セットアップ

### 📋 前提条件

- **Java**: 17以上
- **Maven**: 3.6以上  
- **PostgreSQL**: 12以上
- **Webサーバー**: Tomcat 10以上 または Jetty 11以上

### 🔧 ビルドと実行

#### 1. データベースのセットアップ

詳細は `DATABASE_SETUP.md` を参照してください。

```bash
# PostgreSQLのインストール（macOS）
brew install postgresql
brew services start postgresql

# データベースとユーザーの作成
createdb kintai
psql kintai < src/main/resources/schema.sql
```

#### 2. 環境変数の設定

```bash
export DB_URL="jdbc:postgresql://localhost:5432/kintai"
export DB_USERNAME="kintai_user"
export DB_PASSWORD="kintai_password"
```

#### 3. プロジェクトのビルドと実行

```bash
# 依存関係のダウンロードとコンパイル
mvn clean compile
```

#### 4. 開発環境での実行

```bash
# Jettyサーバーで起動
mvn jetty:run
```

アプリケーションは <http://localhost:8080/kintai> でアクセス可能です。

#### 5. プロダクション用WARファイルの作成

```bash
# WARファイルを作成
mvn clean package
```

`target/kintai.war` ファイルが作成され、Tomcatなどのサーブレットコンテナにデプロイできます。

## 👥 初期ユーザー

システムには以下の初期ユーザーがPostgreSQLデータベースに登録されています：

| ユーザーID | パスワード | 役割 | 状態 |
|------------|------------|------|------|
| `employee1` | `password` | 従業員 | ✅ 有効 |
| `admin1` | `adminpass` | 管理者 | ✅ 有効 |
| `employee2` | `password` | 従業員 | ❌ 無効 |

## 🖥️ 使用方法

### ログイン

1. ブラウザで <http://localhost:8080/kintai> にアクセス
2. 上記の初期ユーザーでログイン

### 従業員の操作

- **出勤記録**: 「出勤」ボタンをクリック
- **退勤記録**: 「退勤」ボタンをクリック
- **履歴確認**: 自分の勤怠履歴を表で確認

### 管理者の操作

- **勤怠管理**: 全従業員の勤怠データを閲覧・管理
- **フィルタリング**: ユーザーや期間で絞り込み検索
- **CSV出力**: 勤怠データをCSVでエクスポート
- **ユーザー管理**: 従業員の追加・編集・削除
- **手動記録**: 勤怠データの手動追加・修正

## 🔧 トラブルシューティング

### コンパイルエラー「シンボルを見つけられません」

```bash
# Maven依存関係を再ダウンロード
mvn clean install

# IDEでプロジェクトをリフレッシュ
# Eclipse: F5キーまたは右クリック→Refresh
# IntelliJ: Ctrl+Shift+O または Maven→Reload project
```

### ポート8080が使用中のエラー

```bash
# 使用中のポートを確認
lsof -i :8080

# プロセスを終了（PIDは上記で確認）
kill -9 <PID>

# または別のポートを使用
mvn jetty:run -Djetty.port=8081
```

## ⚠️ 注意事項

- **データ永続化**: このシステムはPostgreSQLデータベースと連携してデータを永続化します
- **本番環境**: 実際の運用では適切なデータベース設定とセキュリティ対策が必要です
- **セキュリティ**: パスワードはSHA-256でハッシュ化されていますが、本番環境ではより強固な認証システムの実装をお勧めします
- **ログ**: 本番環境では適切なログ出力の実装が必要です
- **接続プール**: HikariCPを使用してデータベース接続を効率的に管理しています

## 🔄 開発・拡張

### アーキテクチャ

- **MVC パターン**: Model（DTO/DAO）、View（JSP）、Controller（Servlet）
- **レイヤー構造**: プレゼンテーション層、ビジネス層、データアクセス層
- **フィルターパターン**: 認証・認可の一元管理

### 拡張可能性

- より高度なデータベース機能（JPA/Hibernateの追加）
- REST API化（JAX-RSの追加）
- フロントエンド現代化（React/Vue.js）
- テスト追加（JUnit、Mockito）
- 他のデータベースへの対応（MySQL、Oracle等）

## 📄 ライセンス

このプロジェクトはMITライセンスの下で公開されています。

---

**開発者向け情報**: このプロジェクトはJava EE学習用のサンプルアプリケーションとして作成されました。本格的な勤怠管理システムとして使用する場合は、セキュリティ、パフォーマンス、可用性の観点から追加の実装が必要です。
