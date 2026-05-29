package com.chatapp.server.core;

import com.chatapp.server.controller.ServerController;
import com.chatapp.server.dao.MessageDAO;
import com.chatapp.server.dao.UserDAO;
import com.chatapp.shared.Message;
import com.chatapp.shared.MessageType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

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
        else if (msg.getType() == MessageType.GET_ONLINE_USERS) {
            serverCore.broadcastOnlineUsers();
        }
        else if (msg.getType() == MessageType.PRIVATE_CHAT || msg.getType() == MessageType.TYPING || msg.getType() == MessageType.STOP_TYPING) {
            String receiver = msg.getReceiver();
            
            // Nếu là gửi tin nhắn thực sự, lưu vào database
            if (msg.getType() == MessageType.PRIVATE_CHAT) {
                int msgId = MessageDAO.saveMessage(msg.getSender(), receiver, msg.getContent());
                msg.setPayload(msgId); // Gửi kèm ID để Client dùng khi xóa
            }
            
            ClientHandler receiverHandler = serverCore.clients.get(receiver);
            if (receiverHandler != null) {
                receiverHandler.sendMessage(msg);
            }
        }
        else if (msg.getType() == MessageType.SEND_FILE) {
            String receiver = msg.getReceiver();
            ClientHandler receiverHandler = serverCore.clients.get(receiver);
            if (receiverHandler != null) {
                receiverHandler.sendMessage(msg);
            }
        }
        else if (msg.getType() == MessageType.CREATE_GROUP) {
            String groupName = msg.getContent();
            List<String> members = (List<String>) msg.getPayload();
            serverCore.createGroup(groupName, members);
        }
        else if (msg.getType() == MessageType.GROUP_CHAT) {
            String groupName = msg.getReceiver();
            List<String> members = serverCore.groups.get(groupName);
            if (members != null) {
                for (String member : members) {
                    if (!member.equals(username)) { // Không gửi lại cho người gửi
                        ClientHandler handler = serverCore.clients.get(member);
                        if (handler != null) {
                            handler.sendMessage(msg);
                        }
                    }
                }
            }
        }
        else if (msg.getType() == MessageType.GET_CHAT_HISTORY) {
            String targetUser = msg.getReceiver();
            List<Message> history = MessageDAO.getChatHistory(username, targetUser);
            Message res = new Message(MessageType.CHAT_HISTORY_RESPONSE, username, targetUser, "");
            res.setPayload(history);
            sendMessage(res);
        }
        else if (msg.getType() == MessageType.DELETE_MESSAGE) {
            int msgId = (Integer) msg.getPayload();
            MessageDAO.deleteMessage(msgId);
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
