package com.chatapp.shared;
import java.io.Serializable;
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    private MessageType type;
    private String sender;
    private String receiver;
    private String content;
    private String fileName;
    private byte[] fileData;
    private Object payload;
    public Message(MessageType type) { this.type = type; }
    public Message(MessageType type, String sender, String receiver, String content) {
        this.type = type;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
    }
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public byte[] getFileData() { return fileData; }
    public void setFileData(byte[] fileData) { this.fileData = fileData; }
    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }
}