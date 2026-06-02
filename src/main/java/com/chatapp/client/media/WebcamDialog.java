package com.chatapp.client.media;
import com.github.sarxos.webcam.Webcam;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.function.Consumer;
public class WebcamDialog {
    private Webcam webcam;
    private boolean isPreviewing = false;
    private boolean isRecording = false;
    private java.util.List<byte[]> videoFrames = new java.util.ArrayList<>();
    public void show(Consumer<Object> onMediaTaken) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Webcam - Chụp ảnh & Quay Video");
        ImageView previewView = new ImageView();
        previewView.setFitWidth(640);
        previewView.setFitHeight(480);
        Button btnCapture = new Button("📸 Chụp ảnh");
        btnCapture.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 20; -fx-cursor: hand;");
        Button btnRecord = new Button("🔴 Quay video");
        btnRecord.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 20; -fx-cursor: hand;");
        btnRecord.setOnAction(e -> {
            if (!isRecording) {
                isRecording = true;
                videoFrames.clear();
                btnRecord.setText("⏹ Đang quay...");
                btnRecord.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 20; -fx-cursor: hand;");
                btnCapture.setDisable(true);
                new Thread(() -> {
                    try { Thread.sleep(3000); } catch (Exception ex) {}
                    if (isRecording) {
                        Platform.runLater(btnRecord::fire);
                    }
                }).start();
            } else {
                isRecording = false;
                isPreviewing = false;
                if (webcam != null && webcam.isOpen()) webcam.close();
                onMediaTaken.accept(videoFrames);
                stage.close();
            }
        });
        HBox controls = new HBox(20, btnCapture, btnRecord);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new javafx.geometry.Insets(15));
        VBox root = new VBox(previewView, controls);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: black;");
        webcam = Webcam.getDefault();
        if (webcam != null) {
            webcam.open();
            isPreviewing = true;
            Thread previewThread = new Thread(() -> {
                while (isPreviewing && webcam.isOpen()) {
                    BufferedImage img = webcam.getImage();
                    if (img != null) {
                        try {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ImageIO.write(img, "jpg", baos);
                            byte[] frameBytes = baos.toByteArray();
                            if (isRecording) {
                                videoFrames.add(frameBytes);
                            }
                            Image fxImg = new Image(new ByteArrayInputStream(frameBytes));
                            Platform.runLater(() -> previewView.setImage(fxImg));
                        } catch (Exception ex) {}
                    }
                    try { Thread.sleep(100); } catch (Exception e) {} 
                }
            });
            previewThread.start();
        } else {
            Alert a = new Alert(Alert.AlertType.ERROR, "Không tìm thấy thiết bị Webcam!");
            a.show();
        }
        btnCapture.setOnAction(e -> {
            isPreviewing = false;
            if (webcam != null && webcam.isOpen()) {
                BufferedImage capture = webcam.getImage();
                webcam.close();
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(capture, "jpg", baos);
                    onMediaTaken.accept(baos.toByteArray());
                } catch(Exception ex) {}
            }
            stage.close();
        });
        stage.setOnCloseRequest(e -> {
            isPreviewing = false;
            if (webcam != null && webcam.isOpen()) {
                webcam.close();
            }
        });
        stage.setScene(new Scene(root, 640, 560));
        stage.show();
    }
}