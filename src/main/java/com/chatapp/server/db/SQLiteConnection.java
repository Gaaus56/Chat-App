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
            System.out.println("Khởi tạo CSDL hoàn tất.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
