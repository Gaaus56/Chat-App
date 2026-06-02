package com.chatapp.server.core;

import com.chatapp.server.controller.ServerController;
import javafx.application.Platform;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import com.chatapp.shared.Message;
import com.chatapp.shared.MessageType;

public class ServerCore implements Runnable {
    private int port;
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private ServerController controller;
    
    // Quản lý các ClientHandler
    public ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    
    // Quản lý các nhóm (Tên nhóm -> Danh sách thành viên)
    public ConcurrentHashMap<String, List<String>> groups = new ConcurrentHashMap<>();

    public ServerCore(int port, ServerController controller) {
        this.port = port;
        this.controller = controller;
        
        // Tải danh sách nhóm chat từ Database
        java.util.Map<String, java.util.List<String>> savedGroups = com.chatapp.server.dao.GroupDAO.loadAllGroups();
        groups.putAll(savedGroups);
        System.out.println("Đã tải " + savedGroups.size() + " nhóm chat từ Database.");
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            isRunning = true;
            
            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                String ip = clientSocket.getInetAddress().getHostAddress();
                
                // Hiển thị lên bảng với tên "Unknown" cho tới khi họ đăng nhập
                String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                controller.addClientToTable(ip, "Unknown", time);
                
                ClientHandler handler = new ClientHandler(clientSocket, this, ip, controller);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            if (isRunning) {
                System.out.println("Lỗi Server Socket: " + e.getMessage());
            }
        }
    }

    public void stopServer() {
        isRunning = false;
        try {
            for (ClientHandler handler : clients.values()) {
                handler.closeConnection();
            }
            clients.clear();
            if (serverSocket != null) {
                serverSocket.close();
            }
            controller.clearTable();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void addAuthenticatedClient(String username, ClientHandler handler) {
        clients.put(username, handler);
        broadcastOnlineUsers();
    }
    
    public void removeClient(String username) {
        if (username != null) {
            clients.remove(username);
            controller.removeClientFromTable(username);
            broadcastOnlineUsers();
        }
    }

    public void broadcastOnlineUsers() {
        List<String> userList = new ArrayList<>(clients.keySet());
        Message msg = new Message(MessageType.ONLINE_USERS_LIST);
        msg.setPayload(userList);
        for (ClientHandler handler : clients.values()) {
            handler.sendMessage(msg);
        }
    }

    public void createGroup(String groupName, List<String> members) {
        groups.put(groupName, members);
        Message msg = new Message(MessageType.ADD_USER_TO_GROUP, "Server", "", groupName);
        msg.setPayload(members);
        for (String member : members) {
            ClientHandler handler = clients.get(member);
            if (handler != null) {
                handler.sendMessage(msg);
            }
        }
    }
}
