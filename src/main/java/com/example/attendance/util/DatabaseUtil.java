package com.example.attendance.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * データベース接続を管理するユーティリティクラス。
 * HikariCPを使用してコネクションプールを管理します。
 */
public class DatabaseUtil {
    private static HikariDataSource dataSource;

    static {
        try {
            initializeDataSource();
            // 初期化時にスキーマを適用
            try (Connection conn = dataSource.getConnection()) {
                executeSchemaScript(conn);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database connection pool", e);
        }
    }

    /**
     * スキーマファイルを実行してデータベースを初期化します。
     * @param conn データベース接続
     */
    public static void executeSchemaScript(Connection conn) {
        try (InputStream is = DatabaseUtil.class.getClassLoader().getResourceAsStream("schema.sql")) {
            if (is == null) {
                System.err.println("schema.sql not found!");
                return;
            }
            
            String script = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(script);
                System.out.println("Database schema has been successfully applied.");
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to execute schema.sql", e);
        }
    }

    /**
     * データソースを初期化します。
     */
    private static void initializeDataSource() {
        HikariConfig config = new HikariConfig();
        
        // 環境変数またはシステムプロパティから設定を取得、デフォルト値を設定
        String jdbcUrl = System.getProperty("db.url", 
                        System.getenv("DB_URL") != null ? System.getenv("DB_URL") : 
                        "jdbc:postgresql://localhost:5432/kintai");
        String username = System.getProperty("db.username", 
                         System.getenv("DB_USERNAME") != null ? System.getenv("DB_USERNAME") : 
                         "kintai_user");
        String password = System.getProperty("db.password", 
                         System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : 
                         "kintai_password");

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");

        // コネクションプールの設定
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        // PostgreSQL固有の設定
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);
    }

    /**
     * データベース接続を取得します。
     * @return データベース接続
     * @throws SQLException SQL例外
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * DataSourceを取得します。
     * @return DataSource
     */
    public static DataSource getDataSource() {
        return dataSource;
    }

    /**
     * データソースを閉じます（アプリケーション終了時に呼ぶ）。
     */
    public static void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
