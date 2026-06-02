package com.chatapp.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class SQLiteConnection {
    // Lưu database tại thư mục chạy app hiện tại
    private static final String DB_URL = "jdbc:sqlite:chatapp.db";

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initialize() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            System.out.println("Đang kết nối tới SQLite Database...");
            // Tạo bảng users
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "username TEXT UNIQUE NOT NULL, " +
                         "password TEXT NOT NULL)");
                         
            // Tạo bảng messages
            stmt.execute("CREATE TABLE IF NOT EXISTS messages (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "sender TEXT NOT NULL, " +
                         "receiver TEXT NOT NULL, " +
                         "content TEXT NOT NULL, " +
                         "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");
                         
            // Tạo bảng chat_groups
            stmt.execute("CREATE TABLE IF NOT EXISTS chat_groups (" +
                         "group_name TEXT PRIMARY KEY, " +
                         "members TEXT NOT NULL)");
                         
            // Thêm các cột bổ sung nếu chưa có (dùng ALTER TABLE)
            try {
                // Thêm last_logout cho users
                stmt.execute("ALTER TABLE users ADD COLUMN last_logout DATETIME DEFAULT '1970-01-01 00:00:00'");
            } catch (Exception ignore) { }
            
            try {
                // Thêm msg_type cho messages để lưu phân loại tin nhắn (TEXT, FILE, VOICE, WEBCAM)
                stmt.execute("ALTER TABLE messages ADD COLUMN msg_type TEXT DEFAULT 'PRIVATE_CHAT'");
            } catch (Exception ignore) { }
            
            System.out.println("Khởi tạo CSDL hoàn tất.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
