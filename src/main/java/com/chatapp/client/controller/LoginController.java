package com.chatapp.client.controller;

import com.chatapp.client.core.ClientCore;
import com.chatapp.client.model.ServerConfig;
import com.chatapp.shared.Message;
import com.chatapp.shared.MessageType;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;

public class LoginController {
    @FXML private Label lblServerInfo;
    @FXML private Label lblTitle;
    @FXML private Button btnAction;
    @FXML private Hyperlink linkToggle;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    
    private boolean isRegisterMode = false;
    
    private ServerConfig config;
    private ClientCore clientCore;

    public boolean initData(ServerConfig config) {
        this.config = config;
        lblServerInfo.setText("Đang kết nối tới: " + config.name + " (" + config.ip + ")");
        
        // Khởi tạo kết nối ngay khi vào màn hình Đăng nhập
        try {
            clientCore = new ClientCore(config.ip, config.port);
            clientCore.setOnMessageReceived(this::handleServerResponse);
            
            // Lắng nghe sự kiện mất kết nối Server
            clientCore.setOnDisconnected(() -> {
                Platform.runLater(() -> {
                    showAlert("Lỗi", "Đã mất kết nối tới Server!");
                    returnToConfig();
                });
            });
            
            // Xử lý sự kiện nhấn phím Enter
            txtPassword.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    handleAction();
                }
            });
            txtUsername.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    handleAction();
                }
            });
            return true;
        } catch (Exception e) {
            showAlert("Lỗi Kết Nối", "Không thể kết nối đến Server. Vui lòng kiểm tra lại IP/Port và đảm bảo Server đang chạy.");
            return false;
        }
    }

    @FXML
    private void toggleMode() {
        isRegisterMode = !isRegisterMode;
        if (isRegisterMode) {
            lblTitle.setText("ĐĂNG KÝ");
            btnAction.setText("Tạo Tài Khoản");
            linkToggle.setText("Đã có tài khoản? Đăng nhập ngay");
        } else {
            lblTitle.setText("ĐĂNG NHẬP");
            btnAction.setText("Đăng Nhập");
            linkToggle.setText("Chưa có tài khoản? Đăng ký ngay");
        }
    }

    @FXML
    private void handleAction() {
        if (clientCore == null) return;
        
        String user = txtUsername.getText().trim();
        String pass = txtPassword.getText().trim();
        if (user.isEmpty() || pass.isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập đầy đủ Tài khoản và Mật khẩu!");
            return;
        }
        
        Message msg = new Message(isRegisterMode ? MessageType.REGISTER : MessageType.LOGIN);
        msg.setPayload(new String[]{user, pass});
        clientCore.sendMessage(msg);
    }
    
    private void handleServerResponse(Message msg) {
        Platform.runLater(() -> {
            switch (msg.getType()) {
                case LOGIN_SUCCESS:
                    openMainChat();
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

    private void openMainChat() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chatapp/client/MainChatView.fxml"));
            Parent root = loader.load();
            
            MainChatController controller = loader.getController();
            controller.initData(clientCore, txtUsername.getText().trim());
            
            Stage stage = (Stage) lblServerInfo.getScene().getWindow();
            stage.setTitle("Chat App - Xin chào " + txtUsername.getText().trim());
            stage.setScene(new Scene(root, 800, 600));
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Lỗi", "Không thể mở giao diện Chat: " + e.getMessage());
        }
    }

    private void returnToConfig() {
        try {
            if (clientCore != null) {
                clientCore.closeConnection();
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chatapp/client/ConfigView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) lblServerInfo.getScene().getWindow();
            stage.setTitle("Quản Lý Máy Chủ - Chat App");
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, content);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
