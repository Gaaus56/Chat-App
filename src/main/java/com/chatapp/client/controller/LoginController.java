package com.chatapp.client.controller;

import com.chatapp.client.core.ClientCore;
import com.chatapp.client.model.ServerConfig;
import com.chatapp.shared.Message;
import com.chatapp.shared.MessageType;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML private Label lblServerInfo;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    
    private ServerConfig config;
    private ClientCore clientCore;

    public void initData(ServerConfig config) {
        this.config = config;
        lblServerInfo.setText("Đang kết nối tới: " + config.name + " (" + config.ip + ")");
        
        // Khởi tạo kết nối ngay khi vào màn hình Đăng nhập
        try {
            clientCore = new ClientCore(config.ip, config.port);
            clientCore.setOnMessageReceived(this::handleServerResponse);
        } catch (Exception e) {
            showAlert("Lỗi Kết Nối", "Không thể kết nối đến Server. Vui lòng kiểm tra lại IP/Port và đảm bảo Server đang chạy.");
        }
    }

    @FXML
    private void handleLogin() {
        if (clientCore == null) return;
        
        String user = txtUsername.getText().trim();
        String pass = txtPassword.getText().trim();
        if (user.isEmpty() || pass.isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập đầy đủ Tài khoản và Mật khẩu!");
            return;
        }
        
        Message msg = new Message(MessageType.LOGIN);
        msg.setPayload(new String[]{user, pass});
        clientCore.sendMessage(msg);
    }

    @FXML
    private void handleRegister() {
        if (clientCore == null) return;
        
        String user = txtUsername.getText().trim();
        String pass = txtPassword.getText().trim();
        if (user.isEmpty() || pass.isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập đầy đủ Tài khoản và Mật khẩu!");
            return;
        }
        
        Message msg = new Message(MessageType.REGISTER);
        msg.setPayload(new String[]{user, pass});
        clientCore.sendMessage(msg);
    }
    
    private void handleServerResponse(Message msg) {
        Platform.runLater(() -> {
            switch (msg.getType()) {
                case LOGIN_SUCCESS:
                    showAlert("Thành công", "Đăng nhập thành công! (Chuẩn bị mở màn hình Chat ở Phase 3)");
                    break;
                case LOGIN_FAIL:
                    showAlert("Thất bại", msg.getContent() != null ? msg.getContent() : "Đăng nhập thất bại!");
                    break;
                case REGISTER_SUCCESS:
                    showAlert("Thành công", "Đăng ký thành công! Bạn có thể đăng nhập ngay bây giờ.");
                    break;
                case REGISTER_FAIL:
                    showAlert("Thất bại", msg.getContent() != null ? msg.getContent() : "Đăng ký thất bại!");
                    break;
            }
        });
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, content);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
