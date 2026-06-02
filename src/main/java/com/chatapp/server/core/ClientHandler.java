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
        else if (msg.getType() == MessageType.GET_UNREAD_MESSAGES) {
            java.util.List<String> unread = userDAO.getUnreadSenders(username, serverCore.groups);
            if (!unread.isEmpty()) {
                Message unreadMsg = new Message(MessageType.UNREAD_MESSAGES_LIST);
                unreadMsg.setPayload(unread);
                sendMessage(unreadMsg);
            }
        }
        else if (msg.getType() == MessageType.GET_ALL_USERS) {
            com.chatapp.server.dao.UserDAO userDAO = new com.chatapp.server.dao.UserDAO();
            java.util.List<String> allUsers = userDAO.getAllUsers();
            Message res = new Message(MessageType.ALL_USERS_LIST, "Server", username, "");
            res.setPayload(allUsers);
            sendMessage(res);
        }
        else if (msg.getType() == MessageType.GET_GROUPS) {
            for (java.util.Map.Entry<String, java.util.List<String>> entry : serverCore.groups.entrySet()) {
                if (entry.getValue().contains(username)) {
                    Message groupMsg = new Message(MessageType.ADD_USER_TO_GROUP, "Server", "", entry.getKey());
                    sendMessage(groupMsg);
                }
            }
        }
        else if (msg.getType() == MessageType.PRIVATE_CHAT) {
            String receiver = msg.getReceiver();
            int msgId = MessageDAO.saveMessage(msg.getSender(), receiver, msg.getContent(), msg.getType());
            msg.setPayload(msgId);
            
            ClientHandler receiverHandler = serverCore.clients.get(receiver);
            if (receiverHandler != null) {
                receiverHandler.sendMessage(msg);
            }
        }
        else if (msg.getType() == MessageType.TYPING || msg.getType() == MessageType.STOP_TYPING) {
            String receiver = msg.getReceiver();
            if (serverCore.groups.containsKey(receiver)) {
                List<String> members = serverCore.groups.get(receiver);
                for (String member : members) {
                    if (!member.equals(username)) {
                        ClientHandler handler = serverCore.clients.get(member);
                        if (handler != null) handler.sendMessage(msg);
                    }
                }
            } else {
                ClientHandler receiverHandler = serverCore.clients.get(receiver);
                if (receiverHandler != null) receiverHandler.sendMessage(msg);
            }
        }
        else if (msg.getType() == MessageType.SEND_FILE || msg.getType() == MessageType.VOICE_MESSAGE || msg.getType() == MessageType.WEBCAM_FRAME) {
            String receiver = msg.getReceiver();
            
            // LƯU FILE RA Ổ CỨNG SERVER
            String fileName = "media_" + System.currentTimeMillis() + ".dat";
            String filePath = "server_media/" + fileName;
            try {
                java.io.FileOutputStream fos = new java.io.FileOutputStream(filePath);
                java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(fos);
                oos.writeObject(msg.getPayload());
                oos.close();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            // LƯU ĐƯỜNG DẪN VÀO DB
            // Đối với SEND_FILE, msg.getContent() chứa tên file gốc, ta gộp lại: originalName|filePath
            String dbContent = msg.getContent();
            if (msg.getType() == MessageType.SEND_FILE) {
                dbContent = msg.getContent() + "|" + filePath;
            } else {
                dbContent = filePath;
            }
            int msgId = MessageDAO.saveMessage(username, receiver, dbContent, msg.getType());
            msg.setPayload(new Object[]{ msgId, msg.getPayload() }); 
            
            if (serverCore.groups.containsKey(receiver)) {
                List<String> members = serverCore.groups.get(receiver);
                for (String member : members) {
                    ClientHandler handler = serverCore.clients.get(member);
                    if (handler != null) handler.sendMessage(msg);
                }
            } else {
                ClientHandler receiverHandler = serverCore.clients.get(receiver);
                if (receiverHandler != null) {
                    receiverHandler.sendMessage(msg);
                }
                sendMessage(msg);
            }
        }
        else if (msg.getType() == MessageType.CREATE_GROUP) {
            String groupName = msg.getContent();
            List<String> members = (List<String>) msg.getPayload();
            serverCore.createGroup(groupName, members);
            com.chatapp.server.dao.GroupDAO.saveGroup(groupName, members);
        }
        else if (msg.getType() == MessageType.GROUP_CHAT) {
            String groupName = msg.getReceiver();
            int msgId = com.chatapp.server.dao.MessageDAO.saveMessage(msg.getSender(), groupName, msg.getContent(), msg.getType());
            msg.setPayload(msgId);
            
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
        else if (msg.getType() == MessageType.UPDATE_GROUP_MEMBERS) {
            String groupName = msg.getReceiver();
            List<String> newMembers = (List<String>) msg.getPayload();
            List<String> oldMembers = serverCore.groups.get(groupName);
            if (oldMembers != null) {
                // Ai bị xóa khỏi nhóm
                for (String old : oldMembers) {
                    if (!newMembers.contains(old)) {
                        Message rmMsg = new Message(MessageType.REMOVE_USER_FROM_GROUP, "Server", "", groupName);
                        ClientHandler h = serverCore.clients.get(old);
                        if (h != null) h.sendMessage(rmMsg);
                    }
                }
                // Ai được thêm vào nhóm
                for (String newMem : newMembers) {
                    if (!oldMembers.contains(newMem)) {
                        Message addMsg = new Message(MessageType.ADD_USER_TO_GROUP, "Server", "", groupName);
                        ClientHandler h = serverCore.clients.get(newMem);
                        if (h != null) h.sendMessage(addMsg);
                    }
                }
                serverCore.groups.put(groupName, newMembers);
                com.chatapp.server.dao.GroupDAO.saveGroup(groupName, newMembers);
            }
        }
        else if (msg.getType() == MessageType.GET_GROUP_MEMBERS) {
            String groupName = msg.getReceiver();
            List<String> members = serverCore.groups.get(groupName);
            Message res = new Message(MessageType.GROUP_MEMBERS_RESPONSE, "Server", groupName, "");
            res.setPayload(members);
            sendMessage(res);
        }
        else if (msg.getType() == MessageType.DISBAND_GROUP) {
            String groupName = msg.getReceiver();
            List<String> members = serverCore.groups.get(groupName);
            if (members != null) {
                // Gửi thông báo REMOVE_USER_FROM_GROUP đến tất cả thành viên để họ tự động xóa tab
                for (String mem : members) {
                    ClientHandler h = serverCore.clients.get(mem);
                    if (h != null) {
                        Message rmMsg = new Message(MessageType.REMOVE_USER_FROM_GROUP, "Server", "", groupName);
                        h.sendMessage(rmMsg);
                    }
                }
                serverCore.groups.remove(groupName);
                com.chatapp.server.dao.GroupDAO.deleteGroup(groupName);
            }
        }
        else if (msg.getType() == MessageType.GET_CHAT_HISTORY) {
            String targetUser = msg.getReceiver();
            boolean isGroup = serverCore.groups.containsKey(targetUser);
            List<Message> history = com.chatapp.server.dao.MessageDAO.getChatHistory(username, targetUser, isGroup);
            Message res = new Message(MessageType.CHAT_HISTORY_RESPONSE, username, targetUser, "");
            res.setPayload(history);
            sendMessage(res);
        }
        else if (msg.getType() == MessageType.DELETE_MESSAGE) {
            int msgId = (Integer) msg.getPayload();
            MessageDAO.deleteMessage(msgId);
            
            String receiver = msg.getReceiver();
            if (serverCore.groups.containsKey(receiver)) {
                List<String> members = serverCore.groups.get(receiver);
                for (String member : members) {
                    if (!member.equals(username)) {
                        ClientHandler handler = serverCore.clients.get(member);
                        if (handler != null) handler.sendMessage(msg);
                    }
                }
            } else {
                ClientHandler receiverHandler = serverCore.clients.get(receiver);
                if (receiverHandler != null) receiverHandler.sendMessage(msg);
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
        if (username != null) {
            userDAO.updateLogoutTime(username);
        }
        serverCore.removeClient(username, this);
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
