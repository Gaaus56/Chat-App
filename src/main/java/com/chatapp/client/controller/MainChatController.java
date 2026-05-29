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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainChatController {
    @FXML private ListView<String> listOnlineUsers;
    @FXML private TabPane tabPaneChats;

    private ClientCore clientCore;
    private String myUsername;
    private ObservableList<String> onlineUsers = FXCollections.observableArrayList();
    private Map<String, VBox> chatHistories = new HashMap<>();
    private Map<String, Node> typingNodes = new HashMap<>(); // Chứa bong bóng 'đang gõ...'

    public void initData(ClientCore clientCore, String myUsername) {
        this.clientCore = clientCore;
        this.myUsername = myUsername;
        
        listOnlineUsers.setItems(onlineUsers);
        
        // Nhận gói tin mới cho Màn hình Chat
        clientCore.setOnMessageReceived(this::handleServerMessage);
        
        // Yêu cầu tải danh sách user
        clientCore.sendMessage(new Message(MessageType.GET_ONLINE_USERS));
        
        // Bắt sự kiện click 1 lần vào user online để mở Tab
        listOnlineUsers.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                String selectedUser = listOnlineUsers.getSelectionModel().getSelectedItem();
                if (selectedUser != null && !selectedUser.equals(myUsername)) {
                    openChatTab(selectedUser);
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
                    openChatTab(sender); // Nếu chưa mở tab thì tự động mở
                    VBox history = chatHistories.get(sender);
                    if (history != null) {
                        hideTypingIndicator(sender); // Ẩn bong bóng đang gõ đi
                        addMessageBubble(history, msg.getContent(), false);
                    }
                    break;
                case TYPING:
                    showTypingIndicator(msg.getSender());
                    break;
                case STOP_TYPING:
                    hideTypingIndicator(msg.getSender());
                    break;
                default:
                    break;
            }
        });
    }

    private void openChatTab(String targetUser) {
        // Nếu đã có tab của người này thì focus vào nó
        for (Tab t : tabPaneChats.getTabs()) {
            if (t.getText().equals(targetUser)) {
                tabPaneChats.getSelectionModel().select(t);
                return;
            }
        }

        // Tạo giao diện cho Tab mới
        Tab tab = new Tab(targetUser);
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
        
        // Tính năng Đang soạn tin...
        txtInput.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.isEmpty() && oldText.isEmpty()) {
                clientCore.sendMessage(new Message(MessageType.TYPING, myUsername, targetUser, ""));
            } else if (newText.isEmpty() && !oldText.isEmpty()) {
                clientCore.sendMessage(new Message(MessageType.STOP_TYPING, myUsername, targetUser, ""));
            }
        });
        
        // Tính năng Shift+Enter xuống dòng, Enter gửi tin nhắn
        txtInput.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (event.isShiftDown()) {
                    txtInput.appendText("\n");
                } else {
                    event.consume(); // Ngăn tự động xuống dòng
                    sendChatMessage(targetUser, txtInput, chatBox);
                }
            }
        });
        
        Button btnSend = new Button("Gửi");
        btnSend.setMinWidth(60); // Đảm bảo nút đủ to không bị biến thành 3 chấm
        btnSend.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnSend.setOnAction(ev -> sendChatMessage(targetUser, txtInput, chatBox));
        
        HBox inputArea = new HBox(10, txtInput, btnSend);
        HBox.setHgrow(txtInput, Priority.ALWAYS);
        
        root.getChildren().addAll(scrollPane, emojiBox, inputArea);
        tab.setContent(root);
        
        // Quản lý dọn dẹp khi đóng tab
        tab.setOnClosed(e -> chatHistories.remove(targetUser));
        chatHistories.put(targetUser, chatBox);
        
        tabPaneChats.getTabs().add(tab);
        tabPaneChats.getSelectionModel().select(tab);
    }

    private void sendChatMessage(String targetUser, TextArea txtInput, VBox chatBox) {
        String content = txtInput.getText().trim();
        if (!content.isEmpty()) {
            Message msg = new Message(MessageType.PRIVATE_CHAT, myUsername, targetUser, content);
            clientCore.sendMessage(msg);
            addMessageBubble(chatBox, content, true);
            txtInput.clear();
        }
    }

    private void addMessageBubble(VBox chatBox, String text, boolean isMine) {
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
