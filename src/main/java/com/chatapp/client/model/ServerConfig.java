package com.chatapp.client.model;

import java.io.Serializable;

public class ServerConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public String name;
    public String ip;
    public int port;

    public ServerConfig(String name, String ip, int port) {
        this.name = name;
        this.ip = ip;
        this.port = port;
    }

    @Override
    public String toString() {
        return name + " (" + ip + ":" + port + ")";
    }
}
