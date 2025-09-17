package com.example.test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import com.example.attendance.util.DatabaseUtil;

public class DatabaseTest {
    public static void main(String[] args) {
        try {
            System.out.println("Testing database connection...");
            Connection conn = DatabaseUtil.getConnection();
            System.out.println("Connection successful!");

            // Test query
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
            if (rs.next()) {
                System.out.println("Users count: " + rs.getInt(1));
            }

            // List users
            rs = stmt.executeQuery("SELECT username, role, enabled FROM users");
            System.out.println("Users:");
            while (rs.next()) {
                System.out.println("- " + rs.getString("username") + " (" + rs.getString("role") + ") enabled: " + rs.getBoolean("enabled"));
            }

            rs.close();
            stmt.close();
            conn.close();
            System.out.println("Test completed successfully!");

        } catch (Exception e) {
            System.err.println("Database test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}