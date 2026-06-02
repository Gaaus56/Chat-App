package com.chatapp.client.controller;

import com.chatapp.client.model.ServerConfig;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.*;
import java.util.ArrayList;

public class ConfigController {
    @FXML private ListView<ServerConfig> listServers;
    @FXML private TextField txtName;
    @FXML private TextField txtIp;
    @FXML private TextField txtPort;

    private ObservableList<ServerConfig> servers = FXCollections.observableArrayList();
    private final String CONFIG_FILE = "servers.dat";

    @FXML
    public void initialize() {
        listServers.setItems(servers);
        loadConfigs();
        
        // Cập nhật textfield khi click vào list
        listServers.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                txtName.setText(newVal.name);
                txtIp.setText(newVal.ip);
                txtPort.setText(String.valueOf(newVal.port));
            }
        });
    }

    @FXML
    private void handleAdd() {
        try {
            int port = Integer.parseInt(txtPort.getText());
            ServerConfig config = new ServerConfig(txtName.getText(), txtIp.getText(), port);
            servers.add(config);
            saveConfigs();
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Port phải là số!");
        }
    }

    @FXML
    private void handleEdit() {
        ServerConfig selected = listServers.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                selected.name = txtName.getText();
                selected.ip = txtIp.getText();
                selected.port = Integer.parseInt(txtPort.getText());
                listServers.refresh();
                saveConfigs();
            } catch (NumberFormatException e) {
                showAlert("Lỗi", "Port phải là số!");
            }
        }
    }

    @FXML
    private void handleDelete() {
        ServerConfig selected = listServers.getSelectionModel().getSelectedItem();
        if (selected != null) {
            servers.remove(selected);
            saveConfigs();
            txtName.clear(); txtIp.clear(); txtPort.clear();
        }
    }

    @FXML
    private void handleConnect() {
        ServerConfig selected = listServers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Thông báo", "Vui lòng chọn một Server để kết nối!");
            return;
        }

        try {
            // Mở màn hình Login và truyền thông tin Server
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chatapp/client/LoginView.fxml"));
            Parent root = loader.load();
            
            LoginController controller = loader.getController();
            boolean success = controller.initData(selected);
            
            if (success) {
                Stage stage = (Stage) listServers.getScene().getWindow();
                stage.setTitle("Đăng nhập - " + selected.name);
                stage.setScene(new Scene(root));
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Lỗi", "Không thể mở giao diện Đăng nhập.");
        }
    }

    private void saveConfigs() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(CONFIG_FILE))) {
            oos.writeObject(new ArrayList<>(servers));
        } catch (Exception e) { e.printStackTrace(); }
    }

    @SuppressWarnings("unchecked")
    private void loadConfigs() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(CONFIG_FILE))) {
            ArrayList<ServerConfig> list = (ArrayList<ServerConfig>) ois.readObject();
            servers.addAll(list);
        } catch (Exception e) {
            // Lần đầu chưa có file
            servers.add(new ServerConfig("Server Mặc định", "127.0.0.1", 9999));
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, content);
        alert.setTitle(title);
        alert.showAndWait();
    }
}
