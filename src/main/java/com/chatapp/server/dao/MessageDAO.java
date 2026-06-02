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

    public static int saveMessage(String sender, String receiver, String content, MessageType type) {
        String sql = "INSERT INTO messages (sender, receiver, content, msg_type) VALUES (?, ?, ?, ?)";
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, sender);
            pstmt.setString(2, receiver);
            pstmt.setString(3, content);
            pstmt.setString(4, type.name());
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
                String typeStr = "PRIVATE_CHAT";
                try {
                    typeStr = rs.getString("msg_type");
                    if (typeStr == null) typeStr = isGroup ? "GROUP_CHAT" : "PRIVATE_CHAT";
                } catch (Exception ignore) { }
                
                MessageType type = MessageType.valueOf(typeStr);
                String content = rs.getString("content");
                
                // Nạp payload từ file nếu là media
                Object payload = null;
                if (type == MessageType.SEND_FILE || type == MessageType.VOICE_MESSAGE || type == MessageType.WEBCAM_FRAME) {
                    String filePath = content;
                    if (type == MessageType.SEND_FILE && content.contains("|")) {
                        String[] parts = content.split("\\|");
                        content = parts[0];
                        filePath = parts[1];
                    }
                    try {
                        java.io.FileInputStream fis = new java.io.FileInputStream(filePath);
                        java.io.ObjectInputStream ois = new java.io.ObjectInputStream(fis);
                        payload = ois.readObject();
                        ois.close();
                        fis.close();
                    } catch (Exception e) {
                        System.out.println("Không tìm thấy file media: " + filePath);
                    }
                }
                
                Message msg = new Message(type, rs.getString("sender"), rs.getString("receiver"), content);
                if (payload != null) {
                    msg.setPayload(new Object[]{ rs.getInt("id"), payload });
                } else {
                    msg.setPayload(rs.getInt("id")); // Giữ lại logic cũ nếu ko có payload
                }
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
