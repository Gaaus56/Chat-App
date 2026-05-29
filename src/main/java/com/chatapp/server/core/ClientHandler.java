package com.chatapp.server.core;

import com.chatapp.server.controller.ServerController;
import com.chatapp.shared.Message;
import com.chatapp.shared.MessageType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ServerCore serverCore;
    private String ip;
    private ServerController controller;
    
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username;

    public ClientHandler(Socket socket, ServerCore serverCore, String ip, ServerController controller) {
        this.socket = socket;
        this.serverCore = serverCore;
        this.ip = ip;
        this.controller = controller;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            while (true) {
                Message message = (Message) in.readObject();
                handleMessage(message);
            }
        } catch (Exception e) {
            System.out.println("Client mất kết nối: " + (username == null ? ip : username));
        } finally {
            closeConnection();
        }
    }

    private void handleMessage(Message msg) {
        // Tạm thời ở Phase 1 chỉ mock tính năng Đăng nhập để test TableView
        if (msg.getType() == MessageType.LOGIN) {
            String[] credentials = (String[]) msg.getPayload();
            this.username = credentials[0]; // Lấy username
            
            // Đưa vào danh sách client chính thức
            serverCore.addAuthenticatedClient(username, this);
            
            // Cập nhật lại tên trên Giao diện TableView
            controller.updateClientUsername(ip, username);
            
            // Báo lại client thành công (Dummy)
            sendMessage(new Message(MessageType.LOGIN_SUCCESS));
        }
        else if (msg.getType() == MessageType.LOGOUT) {
            closeConnection();
        }
    }

    public void sendMessage(Message msg) {
        try {
            if (out != null) {
                out.writeObject(msg);
                out.flush();
            }
        } catch (IOException e) {
            closeConnection();
        }
    }

    public void closeConnection() {
        serverCore.removeClient(username);
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
