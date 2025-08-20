package com.example.attendance.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * データベース接続を管理するユーティリティクラス。
 * HikariCPを使用してコネクションプールを管理します。
 */
public class DatabaseUtil {
    private static HikariDataSource dataSource;

    static {
        try {
            initializeDataSource();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database connection pool", e);
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
