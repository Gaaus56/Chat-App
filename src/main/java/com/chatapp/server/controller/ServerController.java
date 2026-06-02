package com.chatapp.server.controller;
import com.chatapp.server.core.ServerCore;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
public class ServerController {
    @FXML private TextField txtPort;
    @FXML private Button btnStart;
    @FXML private Button btnStop;
    @FXML private Label lblStatus;
    @FXML private TableView<ClientModel> tableClients;
    @FXML private TableColumn<ClientModel, String> colIp;
    @FXML private TableColumn<ClientModel, String> colUsername;
    @FXML private TableColumn<ClientModel, String> colConnectTime;
    private ObservableList<ClientModel> clientList = FXCollections.observableArrayList();
    private ServerCore serverCore;
    private Thread serverThread;
    @FXML
    public void initialize() {
        colIp.setCellValueFactory(new PropertyValueFactory<>("ip"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colConnectTime.setCellValueFactory(new PropertyValueFactory<>("connectTime"));
        tableClients.setItems(clientList);
    }
    @FXML
    private void handleStartServer() {
        try {
            int port = Integer.parseInt(txtPort.getText().trim());
            serverCore = new ServerCore(port, this);
            serverThread = new Thread(serverCore);
            serverThread.setDaemon(true); 
            serverThread.start();
            lblStatus.setText("Trạng thái: Đang chạy ở cổng " + port);
            lblStatus.getStyleClass().remove("status-stopped");
            lblStatus.getStyleClass().add("status-running");
            btnStart.setDisable(true);
            btnStop.setDisable(false);
            txtPort.setDisable(true);
        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Port phải là một số nguyên!");
            alert.showAndWait();
        }
    }
    @FXML
    private void handleStopServer() {
        if (serverCore != null) {
            serverCore.stopServer();
        }
        lblStatus.setText("Trạng thái: Đã dừng");
        lblStatus.getStyleClass().remove("status-running");
        lblStatus.getStyleClass().add("status-stopped");
        btnStart.setDisable(false);
        btnStop.setDisable(true);
        txtPort.setDisable(false);
    }
    public void addClientToTable(String ip, String username, String time) {
        Platform.runLater(() -> clientList.add(new ClientModel(ip, username, time)));
    }
    public void removeClientFromTable(String username) {
        Platform.runLater(() -> {
            clientList.removeIf(client -> client.getUsername().equals(username));
        });
    }
    public void updateClientUsername(String ip, String newUsername) {
        Platform.runLater(() -> {
            for (ClientModel client : clientList) {
                if (client.getIp().equals(ip) && client.getUsername().equals("Unknown")) {
                    client.setUsername(newUsername);
                    tableClients.refresh();
                    break;
                }
            }
        });
    }
    public void clearTable() {
        Platform.runLater(() -> clientList.clear());
    }
    public static class ClientModel {
        private String ip;
        private String username;
        private String connectTime;
        public ClientModel(String ip, String username, String connectTime) {
            this.ip = ip;
            this.username = username;
            this.connectTime = connectTime;
        }
        public String getIp() { return ip; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getConnectTime() { return connectTime; }
    }
}