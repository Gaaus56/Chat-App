package com.chatapp.server.core;

import com.chatapp.server.controller.ServerController;
import com.chatapp.shared.Message;
import com.chatapp.shared.MessageType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import com.chatapp.server.dao.UserDAO;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ServerCore serverCore;
    private String ip;
    private ServerController controller;
    private UserDAO userDAO;
    
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username;

    public ClientHandler(Socket socket, ServerCore serverCore, String ip, ServerController controller) {
        this.socket = socket;
        this.serverCore = serverCore;
        this.ip = ip;
        this.controller = controller;
        this.userDAO = new UserDAO();
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
        if (msg.getType() == MessageType.REGISTER) {
            String[] credentials = (String[]) msg.getPayload();
            boolean success = userDAO.register(credentials[0], credentials[1]);
            if (success) {
                sendMessage(new Message(MessageType.REGISTER_SUCCESS));
            } else {
                Message res = new Message(MessageType.REGISTER_FAIL);
                res.setContent("Tài khoản đã tồn tại hoặc có lỗi!");
                sendMessage(res);
            }
        }
        else if (msg.getType() == MessageType.LOGIN) {
            String[] credentials = (String[]) msg.getPayload();
            String reqUsername = credentials[0];
            
            // Kiểm tra xem đã online chưa
            if (serverCore.clients.containsKey(reqUsername)) {
                Message res = new Message(MessageType.LOGIN_FAIL);
                res.setContent("Tài khoản này đang được đăng nhập ở nơi khác!");
                sendMessage(res);
                return;
            }
            
            boolean success = userDAO.login(reqUsername, credentials[1]);
            
            if (success) {
                this.username = reqUsername;
                serverCore.addAuthenticatedClient(username, this);
                controller.updateClientUsername(ip, username);
                sendMessage(new Message(MessageType.LOGIN_SUCCESS));
            } else {
                Message res = new Message(MessageType.LOGIN_FAIL);
                res.setContent("Sai tài khoản hoặc mật khẩu!");
                sendMessage(res);
            }
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
