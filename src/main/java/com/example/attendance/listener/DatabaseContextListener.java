package com.example.attendance.listener;

import com.example.attendance.util.DatabaseUtil;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * アプリケーションのライフサイクルを監視し、
 * 起動時と終了時に必要な処理を実行するリスナー。
 */
@WebListener
public class DatabaseContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // アプリケーション起動時の処理
        // DatabaseUtilの静的初期化によりコネクションプールが作成される
        System.out.println("Database connection pool initialized");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // アプリケーション終了時の処理
        // データベース接続プールを適切に閉じる
        DatabaseUtil.close();
        System.out.println("Database connection pool closed");
    }
}
