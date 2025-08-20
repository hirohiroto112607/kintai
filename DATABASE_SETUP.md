# PostgreSQL Database Configuration
# 勤怠管理システム - データベース設定

## データベース接続設定

### 環境変数での設定（推奨）
以下の環境変数を設定してください：

```bash
export DB_URL="jdbc:postgresql://localhost:5432/kintai"
export DB_USERNAME="kintai_user"
export DB_PASSWORD="kintai_password"
```

### システムプロパティでの設定
JVMオプションとして以下を設定できます：

```bash
-Ddb.url=jdbc:postgresql://localhost:5432/kintai
-Ddb.username=kintai_user
-Ddb.password=kintai_password
```

## PostgreSQLのセットアップ

### 1. PostgreSQLのインストール（macOS）
```bash
brew install postgresql
brew services start postgresql
```

### 2. データベースとユーザーの作成
```sql
# PostgreSQLに管理者としてログイン
psql postgres

# データベースを作成
CREATE DATABASE kintai;

# ユーザーを作成
CREATE USER kintai_user WITH PASSWORD 'kintai_password';

# 権限を付与
GRANT ALL PRIVILEGES ON DATABASE kintai TO kintai_user;

# kintaiデータベースに接続
\c kintai

# スキーマ作成権限を付与
GRANT CREATE ON SCHEMA public TO kintai_user;
```

### 3. スキーマの作成
```bash
# kintaiデータベースにkintai_userとしてログイン
psql -U kintai_user -d kintai

# スキーマファイルを実行
\i src/main/resources/schema.sql
```

## デフォルトユーザー

スキーマ作成時に以下のユーザーが作成されます：

| ユーザー名 | パスワード | 役割 | 有効 |
|-----------|----------|------|------|
| employee1 | password | employee | true |
| admin1 | adminpass | admin | true |
| employee2 | password | employee | false |

## 接続テスト

データベース接続をテストするには：

```bash
psql -U kintai_user -d kintai -c "SELECT username, role, enabled FROM users;"
```

## トラブルシューティング

### 接続エラーが発生する場合

1. PostgreSQLが起動しているか確認
   ```bash
   brew services list | grep postgresql
   ```

2. ユーザーとデータベースが存在するか確認
   ```bash
   psql postgres -c "\du"  # ユーザー一覧
   psql postgres -c "\l"   # データベース一覧
   ```

3. 接続設定を確認
   ```bash
   echo $DB_URL
   echo $DB_USERNAME
   echo $DB_PASSWORD
   ```
