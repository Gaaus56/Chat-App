package com.chatapp.server.dao;
import com.chatapp.server.db.SQLiteConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
public class UserDAO {
    public boolean register(String username, String password) {
        String sql = "INSERT INTO users(username, password) VALUES(?, ?)";
        try (Connection conn = SQLiteConnection.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public boolean login(String username, String password) {
        String sql = "SELECT id FROM users WHERE username = ? AND password = ?";
        try (Connection conn = SQLiteConnection.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            return rs.next(); 
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public java.util.List<String> getAllUsers() {
        java.util.List<String> users = new java.util.ArrayList<>();
        String sql = "SELECT username FROM users";
        try (Connection conn = SQLiteConnection.getConnection(); 
             java.sql.Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(rs.getString("username"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return users;
    }
    public void updateLogoutTime(String username) {
        String sql = "UPDATE users SET last_logout = CURRENT_TIMESTAMP WHERE username = ?";
        try (Connection conn = SQLiteConnection.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public java.util.List<String> getUnreadSenders(String username, java.util.Map<String, java.util.List<String>> groups) {
        java.util.List<String> unread = new java.util.ArrayList<>();
        String sql = "SELECT DISTINCT sender, receiver FROM messages WHERE timestamp > (SELECT last_logout FROM users WHERE username = ?) AND sender != ?";
        try (Connection conn = SQLiteConnection.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, username);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String sender = rs.getString("sender");
                String receiver = rs.getString("receiver");
                if (receiver.equals(username)) {
                    if (!unread.contains(sender)) unread.add(sender);
                } else if (groups.containsKey(receiver) && groups.get(receiver).contains(username)) {
                    if (!unread.contains(receiver)) unread.add(receiver);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return unread;
    }
}