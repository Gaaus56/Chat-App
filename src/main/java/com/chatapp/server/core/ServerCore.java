package com.chatapp.server.core;

import com.chatapp.server.controller.ServerController;
import javafx.application.Platform;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

public class ServerCore implements Runnable {
    private int port;
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private ServerController controller;
    
    // Quản lý các ClientHandler
    public ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public ServerCore(int port, ServerController controller) {
        this.port = port;
        this.controller = controller;
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
    }
    
    public void removeClient(String username) {
        if (username != null) {
            clients.remove(username);
            controller.removeClientFromTable(username);
        }
    }
}
