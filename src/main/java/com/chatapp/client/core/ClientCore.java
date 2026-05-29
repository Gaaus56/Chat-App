package com.chatapp.client.core;

import com.chatapp.shared.Message;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

public class ClientCore {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Consumer<Message> onMessageReceived;
    
    public ClientCore(String ip, int port) throws Exception {
        socket = new Socket(ip, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        
        Thread listenThread = new Thread(() -> {
            try {
                while (true) {
                    Message msg = (Message) in.readObject();
                    if (onMessageReceived != null) {
                        onMessageReceived.accept(msg);
                    }
                }
            } catch (Exception e) {
                System.out.println("Đã ngắt kết nối với server");
            }
        });
        listenThread.setDaemon(true); // Đảm bảo thread tắt khi tắt app
        listenThread.start();
    }
    
    public void setOnMessageReceived(Consumer<Message> callback) {
        this.onMessageReceived = callback;
    }
    
    public void sendMessage(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
