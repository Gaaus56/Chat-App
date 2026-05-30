package com.chatapp.client.media;

import com.github.sarxos.webcam.Webcam;
import com.chatapp.shared.Message;
import com.chatapp.shared.MessageType;
import com.chatapp.client.core.ClientCore;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

public class WebcamService {
    private Webcam webcam;
    private boolean isStreaming;
    private Thread streamThread;
    
    public WebcamService() {
        webcam = Webcam.getDefault();
    }
    
    public void startStreaming(ClientCore clientCore, String myUsername, String target) {
        if (webcam != null && !webcam.isOpen()) {
            webcam.open();
        }
        isStreaming = true;
        
        streamThread = new Thread(() -> {
            while (isStreaming) {
                try {
                    BufferedImage image = webcam.getImage();
                    if (image != null) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(image, "jpg", baos);
                        byte[] imageBytes = baos.toByteArray();
                        
                        Message msg = new Message(MessageType.WEBCAM_FRAME, myUsername, target, "");
                        msg.setPayload(imageBytes);
                        clientCore.sendMessage(msg);
                    }
                    Thread.sleep(100); // ~10 FPS
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        streamThread.start();
    }
    
    public void stopStreaming() {
        isStreaming = false;
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
    }
}
