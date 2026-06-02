package com.chatapp.server.dao;

import com.chatapp.server.db.SQLiteConnection;
import com.chatapp.shared.Message;
import com.chatapp.shared.MessageType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {

    public static int saveMessage(String sender, String receiver, String content) {
        String sql = "INSERT INTO messages (sender, receiver, content) VALUES (?, ?, ?)";
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, sender);
            pstmt.setString(2, receiver);
            pstmt.setString(3, content);
            pstmt.executeUpdate();
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1); // Trả về ID của tin nhắn vừa lưu
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static List<Message> getChatHistory(String user1, String user2, boolean isGroup) {
        List<Message> history = new ArrayList<>();
        String sql;
        if (isGroup) {
            sql = "SELECT * FROM messages WHERE receiver = ? ORDER BY timestamp ASC";
        } else {
            sql = "SELECT * FROM messages WHERE (sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?) ORDER BY timestamp ASC";
        }
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (isGroup) {
                pstmt.setString(1, user2); // user2 là tên group
            } else {
                pstmt.setString(1, user1);
                pstmt.setString(2, user2);
                pstmt.setString(3, user2);
                pstmt.setString(4, user1);
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Message msg = new Message(MessageType.PRIVATE_CHAT, rs.getString("sender"), rs.getString("receiver"), rs.getString("content"));
                msg.setPayload(rs.getInt("id")); // Lưu ID vào payload để có thể xóa sau này
                history.add(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return history;
    }
    
    public static boolean deleteMessage(int messageId) {
        String sql = "DELETE FROM messages WHERE id = ?";
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, messageId);
            pstmt.executeUpdate();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
