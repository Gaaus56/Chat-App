package com.chatapp.client.controller;

import com.chatapp.client.core.ClientCore;
import com.chatapp.shared.Message;
import com.chatapp.shared.MessageType;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.scene.control.Dialog;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainChatController {
    @FXML private ListView<String> listOnlineUsers;
    @FXML private ListView<String> listGroups;
    @FXML private TabPane tabPaneChats;

    private ClientCore clientCore;
    private String myUsername;
    private ObservableList<String> onlineUsers = FXCollections.observableArrayList();
    private ObservableList<String> groupsList = FXCollections.observableArrayList();
    private Map<String, VBox> chatHistories = new HashMap<>();
    private Map<String, Node> typingNodes = new HashMap<>(); // Chứa bong bóng 'đang gõ...'

    public void initData(ClientCore clientCore, String myUsername) {
        this.clientCore = clientCore;
        this.myUsername = myUsername;
        
        listOnlineUsers.setItems(onlineUsers);
        listGroups.setItems(groupsList);
        
        // Nhận gói tin mới cho Màn hình Chat
        clientCore.setOnMessageReceived(this::handleServerMessage);
        
        // Yêu cầu tải danh sách user
        clientCore.sendMessage(new Message(MessageType.GET_ONLINE_USERS));
        
        // Bắt sự kiện click 1 lần vào user online để mở Tab
        listOnlineUsers.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                String selectedUser = listOnlineUsers.getSelectionModel().getSelectedItem();
                if (selectedUser != null && !selectedUser.equals(myUsername)) {
                    openChatTab(selectedUser, false);
                }
            }
        });
        
        // Bắt sự kiện click vào nhóm
        listGroups.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                String selectedGroup = listGroups.getSelectionModel().getSelectedItem();
                if (selectedGroup != null) {
                    openChatTab(selectedGroup, true);
                }
            }
        });
    }

    private void handleServerMessage(Message msg) {
        Platform.runLater(() -> {
            switch (msg.getType()) {
                case ONLINE_USERS_LIST:
                    @SuppressWarnings("unchecked")
                    List<String> users = (List<String>) msg.getPayload();
                    onlineUsers.clear();
                    for (String u : users) {
                        if (!u.equals(myUsername)) {
                            onlineUsers.add(u);
                        }
                    }
                    break;
                case PRIVATE_CHAT:
                    String sender = msg.getSender();
                    openChatTab(sender, false);
                    VBox history = chatHistories.get(sender);
                    if (history != null) {
                        hideTypingIndicator(sender);
                        
                        int msgId = -1;
                        if (msg.getPayload() != null && msg.getPayload() instanceof Integer) {
                            msgId = (Integer) msg.getPayload();
                        }
                        
                        addMessageBubble(history, msg.getContent(), false, msgId, sender);
                    }
                    break;
                case GROUP_CHAT:
                    String targetGroup = msg.getReceiver();
                    openChatTab(targetGroup, true);
                    VBox gHistory = chatHistories.get(targetGroup);
                    if (gHistory != null) {
                        addMessageBubble(gHistory, msg.getSender() + ": " + msg.getContent(), false, -1, targetGroup);
                    }
                    break;
                case ADD_USER_TO_GROUP:
                    String groupName = msg.getContent();
                    if (!groupsList.contains(groupName)) {
                        groupsList.add(groupName);
                    }
                    break;
                case TYPING:
                    showTypingIndicator(msg.getSender());
                    break;
                case STOP_TYPING:
                    hideTypingIndicator(msg.getSender());
                    break;
                case CHAT_HISTORY_RESPONSE:
                    String target = msg.getReceiver(); // Target của request lịch sử
                    VBox targetChatBox = chatHistories.get(target);
                    if (targetChatBox != null) {
                        targetChatBox.getChildren().clear();
                        @SuppressWarnings("unchecked")
                        List<Message> hist = (List<Message>) msg.getPayload();
                        for (Message hMsg : hist) {
                            boolean isMine = hMsg.getSender().equals(myUsername);
                            int hMsgId = hMsg.getPayload() != null ? (Integer) hMsg.getPayload() : -1;
                            addMessageBubble(targetChatBox, hMsg.getContent(), isMine, hMsgId, target);
                        }
                    }
                    break;
                case SEND_FILE:
                    String senderFile = msg.getSender();
                    openChatTab(senderFile, false);
                    VBox boxFile = chatHistories.get(senderFile);
                    if (boxFile != null) {
                        String fileName = msg.getContent();
                        byte[] fileBytes = (byte[]) msg.getPayload();
                        
                        Button btnDownload = new Button("📥 Tải xuống: " + fileName);
                        btnDownload.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
                        btnDownload.setOnAction(e -> {
                            FileChooser saveChooser = new FileChooser();
                            saveChooser.setInitialFileName(fileName);
                            File saveFile = saveChooser.showSaveDialog(tabPaneChats.getScene().getWindow());
                            if (saveFile != null) {
                                try (FileOutputStream fos = new FileOutputStream(saveFile)) {
                                    fos.write(fileBytes);
                                    Alert a = new Alert(Alert.AlertType.INFORMATION, "Đã tải xong file vào: " + saveFile.getAbsolutePath());
                                    a.setHeaderText(null);
                                    a.show();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        });
                        
                        HBox rowFile = new HBox(btnDownload);
                        rowFile.setAlignment(Pos.CENTER_LEFT);
                        boxFile.getChildren().add(rowFile);
                    }
                    break;
                default:
                    break;
            }
        });
    }

    private void openChatTab(String target, boolean isGroup) {
        // Nếu đã có tab của người/nhóm này thì focus vào nó
        for (Tab t : tabPaneChats.getTabs()) {
            if (t.getText().equals(target)) {
                tabPaneChats.getSelectionModel().select(t);
                return;
            }
        }

        // Tạo giao diện cho Tab mới
        Tab tab = new Tab(target);
        VBox root = new VBox(10);
        root.setStyle("-fx-padding: 10; -fx-background-color: #ffffff;");
        
        // Khung cuộn chứa tin nhắn
        ScrollPane scrollPane = new ScrollPane();
        VBox chatBox = new VBox(10);
        chatBox.setStyle("-fx-padding: 10; -fx-background-color: transparent;");
        scrollPane.setContent(chatBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        // Tự động cuộn xuống dưới cùng khi có tin mới
        chatBox.heightProperty().addListener((obs, oldVal, newVal) -> scrollPane.setVvalue(1.0));
        
        HBox emojiBox = new HBox(10);
        String[] emojis = {"😃", "❤️", "👍", "😂", "😭"};
        
        TextArea txtInput = new TextArea();
        txtInput.setPrefRowCount(2);
        txtInput.setWrapText(true);
        txtInput.setPromptText("Nhập tin nhắn... (Shift+Enter xuống dòng, Enter gửi)");
        txtInput.setStyle("-fx-font-size: 14px; -fx-font-family: 'Segoe UI', sans-serif;");
        
        for (String e : emojis) {
            Button btn = new Button(e);
            // Sử dụng Font Emoji chuyên dụng của Windows/Mac để hiển thị màu sắc
            btn.setStyle("-fx-font-family: 'Segoe UI Emoji', 'Apple Color Emoji', 'Noto Color Emoji'; -fx-font-size: 20px; -fx-background-color: transparent; -fx-cursor: hand;");
            btn.setOnAction(ev -> txtInput.appendText(e));
            emojiBox.getChildren().add(btn);
        }
        
        // Tính năng Đang soạn tin... (chỉ dùng cho chat 1-1)
        if (!isGroup) {
            txtInput.textProperty().addListener((obs, oldText, newText) -> {
                if (!newText.isEmpty() && oldText.isEmpty()) {
                    clientCore.sendMessage(new Message(MessageType.TYPING, myUsername, target, ""));
                } else if (newText.isEmpty() && !oldText.isEmpty()) {
                    clientCore.sendMessage(new Message(MessageType.STOP_TYPING, myUsername, target, ""));
                }
            });
        }
        
        // Tính năng Shift+Enter xuống dòng, Enter gửi tin nhắn
        txtInput.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (event.isShiftDown()) {
                    txtInput.appendText("\n");
                } else {
                    event.consume(); // Ngăn tự động xuống dòng
                    sendChatMessage(target, txtInput, chatBox, isGroup);
                }
            }
        });
        
        Button btnSend = new Button("Gửi");
        btnSend.setMinWidth(60);
        btnSend.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnSend.setOnAction(ev -> sendChatMessage(target, txtInput, chatBox, isGroup));
        
        Button btnFile = new Button("📎 Gửi File");
        btnFile.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
        btnFile.setOnAction(ev -> {
            if (isGroup) {
                Alert a = new Alert(Alert.AlertType.WARNING, "Gửi file trong nhóm đang được phát triển.");
                a.show();
                return;
            }
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Chọn file để gửi");
            File file = fileChooser.showOpenDialog(tabPaneChats.getScene().getWindow());
            if (file != null) {
                try {
                    byte[] fileBytes = Files.readAllBytes(file.toPath());
                    Message fileMsg = new Message(MessageType.SEND_FILE, myUsername, target, file.getName());
                    fileMsg.setPayload(fileBytes);
                    clientCore.sendMessage(fileMsg);
                    addMessageBubble(chatBox, "Đã gửi file: " + file.getName(), true, -1, target);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        
        HBox inputArea = new HBox(10, btnFile, txtInput, btnSend);
        HBox.setHgrow(txtInput, Priority.ALWAYS);
        
        root.getChildren().addAll(scrollPane, emojiBox, inputArea);
        tab.setContent(root);
        
        // Quản lý dọn dẹp khi đóng tab
        tab.setOnClosed(e -> chatHistories.remove(target));
        chatHistories.put(target, chatBox);
        
        tabPaneChats.getTabs().add(tab);
        tabPaneChats.getSelectionModel().select(tab);
        
        // Gửi yêu cầu lấy lịch sử chat (chỉ cho chat riêng)
        if (!isGroup) {
            clientCore.sendMessage(new Message(MessageType.GET_CHAT_HISTORY, myUsername, target, ""));
        }
    }

    private void sendChatMessage(String target, TextArea txtInput, VBox chatBox, boolean isGroup) {
        String content = txtInput.getText().trim();
        if (!content.isEmpty()) {
            MessageType type = isGroup ? MessageType.GROUP_CHAT : MessageType.PRIVATE_CHAT;
            Message msg = new Message(type, myUsername, target, content);
            clientCore.sendMessage(msg);
            // Tin nhắn vừa gửi đi chưa có ID trong DB (msgId = -1)
            addMessageBubble(chatBox, content, true, -1, target);
            txtInput.clear();
        }
    }

    private void addMessageBubble(VBox chatBox, String text, boolean isMine, int msgId, String targetUser) {
        TextFlow textFlow = new TextFlow();
        
        // Regex nhận diện Emoji (kể cả trái tim nguyên chuẩn)
        Pattern pattern = Pattern.compile("([\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+|❤️)");
        Matcher matcher = pattern.matcher(text);
        
        int lastEnd = 0;
        while (matcher.find()) {
            // Phần Text bình thường (Tiếng Việt) trước Emoji
            if (matcher.start() > lastEnd) {
                Text normalText = new Text(text.substring(lastEnd, matcher.start()));
                normalText.setStyle("-fx-font-family: 'Segoe UI', sans-serif; -fx-font-size: 14px;");
                normalText.setFill(isMine ? Color.WHITE : Color.BLACK);
                textFlow.getChildren().add(normalText);
            }
            // Phần Emoji
            Text emojiText = new Text(matcher.group());
            emojiText.setStyle("-fx-font-family: 'Segoe UI Emoji', 'Apple Color Emoji'; -fx-font-size: 18px;");
            emojiText.setFill(isMine ? Color.WHITE : Color.BLACK);
            textFlow.getChildren().add(emojiText);
            
            lastEnd = matcher.end();
        }
        // Phần Text còn lại sau cùng
        if (lastEnd < text.length()) {
            Text normalText = new Text(text.substring(lastEnd));
            normalText.setStyle("-fx-font-family: 'Segoe UI', sans-serif; -fx-font-size: 14px;");
            normalText.setFill(isMine ? Color.WHITE : Color.BLACK);
            textFlow.getChildren().add(normalText);
        }
        
        HBox bubble = new HBox(textFlow);
        bubble.setMaxWidth(300);
        bubble.setStyle("-fx-padding: 8 12; -fx-background-radius: 15; " +
                       (isMine ? "-fx-background-color: #0084ff;" : "-fx-background-color: #e4e6eb;"));
        
        HBox row = new HBox(bubble);
        row.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        
        // Menu xóa dòng lịch sử (ContextMenu)
        ContextMenu contextMenu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("Xóa dòng lịch sử chat");
        deleteItem.setOnAction(e -> {
            if (msgId != -1) {
                Message delMsg = new Message(MessageType.DELETE_MESSAGE, myUsername, targetUser, "");
                delMsg.setPayload(msgId);
                clientCore.sendMessage(delMsg);
            }
            chatBox.getChildren().remove(row);
        });
        contextMenu.getItems().add(deleteItem);
        
        row.setOnContextMenuRequested(e -> {
            contextMenu.show(row, e.getScreenX(), e.getScreenY());
        });
        
        // Hiệu ứng Fade In (mượt mà)
        row.setOpacity(0.0);
        chatBox.getChildren().add(row);
        
        FadeTransition ft = new FadeTransition(Duration.millis(300), row);
        ft.setToValue(1.0);
        ft.play();
    }
    
    private void showTypingIndicator(String sender) {
        VBox chatBox = chatHistories.get(sender);
        if (chatBox != null && !typingNodes.containsKey(sender)) {
            Label lblTyping = new Label("💬 " + sender + " đang soạn tin...");
            lblTyping.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic; -fx-font-size: 13px;");
            HBox row = new HBox(lblTyping);
            row.setAlignment(Pos.CENTER_LEFT);
            
            // Hiệu ứng Fade In cho typing
            row.setOpacity(0.0);
            chatBox.getChildren().add(row);
            
            FadeTransition ft = new FadeTransition(Duration.millis(300), row);
            ft.setToValue(1.0);
            ft.play();
            
            typingNodes.put(sender, row);
        }
    }

    private void hideTypingIndicator(String sender) {
        VBox chatBox = chatHistories.get(sender);
        Node typingNode = typingNodes.remove(sender);
        if (chatBox != null && typingNode != null) {
            chatBox.getChildren().remove(typingNode);
        }
    }

    @FXML
    private void handleCreateGroup() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Tạo Nhóm Mới");
        dialog.setHeaderText("Đặt tên nhóm và chọn thành viên");

        ButtonType createButtonType = new ButtonType("Tạo", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        VBox content = new VBox(10);
        TextField txtGroupName = new TextField();
        txtGroupName.setPromptText("Nhập tên nhóm...");
        
        ListView<CheckBox> userChecks = new ListView<>();
        for (String user : onlineUsers) {
            CheckBox cb = new CheckBox(user);
            userChecks.getItems().add(cb);
        }
        
        content.getChildren().addAll(new Label("Tên nhóm:"), txtGroupName, new Label("Thành viên:"), userChecks);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                return txtGroupName.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(groupName -> {
            if (!groupName.trim().isEmpty()) {
                List<String> members = new java.util.ArrayList<>();
                members.add(myUsername);
                for (CheckBox cb : userChecks.getItems()) {
                    if (cb.isSelected()) members.add(cb.getText());
                }
                if (members.size() > 1) {
                    Message msg = new Message(MessageType.CREATE_GROUP, myUsername, "", groupName);
                    msg.setPayload(members);
                    clientCore.sendMessage(msg);
                }
            }
        });
    }

    @FXML
    private void handleLogout() {
        clientCore.sendMessage(new Message(MessageType.LOGOUT));
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chatapp/client/ConfigView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) listOnlineUsers.getScene().getWindow();
            stage.setTitle("Quản Lý Máy Chủ - Chat App");
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
