package com.chatapp.server.dao;

import com.chatapp.server.db.SQLiteConnection;
import java.sql.*;
import java.util.*;

public class GroupDAO {
    public static void saveGroup(String groupName, List<String> members) {
        String sql = "INSERT OR REPLACE INTO chat_groups (group_name, members) VALUES (?, ?)";
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, groupName);
            pstmt.setString(2, String.join(",", members));
            pstmt.executeUpdate();
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }
    
    public static Map<String, List<String>> loadAllGroups() {
        Map<String, List<String>> map = new HashMap<>();
        String sql = "SELECT * FROM chat_groups";
        try (Connection conn = SQLiteConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String name = rs.getString("group_name");
                String mems = rs.getString("members");
                List<String> memberList = new ArrayList<>(Arrays.asList(mems.split(",")));
                map.put(name, memberList);
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
        return map;
    }
    
    public static void deleteGroup(String groupName) {
        String sql = "DELETE FROM chat_groups WHERE group_name = ?";
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, groupName);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
