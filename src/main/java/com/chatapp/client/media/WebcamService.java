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
    public byte[] takePicture() {
        if (webcam == null) return null;
        try {
            if (!webcam.isOpen()) {
                webcam.open();
            }
            BufferedImage image = webcam.getImage();
            webcam.close();
            if (image != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "jpg", baos);
                return baos.toByteArray();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}