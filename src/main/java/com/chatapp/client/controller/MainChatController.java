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
    @FXML private ListView<String> listAllUsers;
    @FXML private ListView<String> listGroups;
    @FXML private TabPane tabPaneChats;

    private ClientCore clientCore;
    private String myUsername;
    private ObservableList<String> onlineUsers = FXCollections.observableArrayList();
    private ObservableList<String> allUsersObs = FXCollections.observableArrayList();
    private Map<String, Boolean> unreadMessages = new HashMap<>();
    private ObservableList<String> groupsList = FXCollections.observableArrayList();
    private Map<String, VBox> chatHistories = new HashMap<>();
    private Map<String, Node> typingNodes = new HashMap<>(); // Chứa bong bóng 'đang gõ...'
    
    // Lưu trữ giao diện hiển thị Webcam (đã xóa)
    // private Map<String, javafx.stage.Stage> videoStages = new HashMap<>();

    
    // Mảng 35 Emoji được tải về
    private static final String[] ALL_EMOJIS = {
        "1f600","1f601","1f602","1f603","1f604","1f605","1f606","1f609","1f60a","1f60b",
        "1f60e","1f60d","1f618","1f617","1f619","1f61a","1f642","1f914","1f610","1f611",
        "1f636","1f644","1f60f","1f623","1f625","1f62e","1f62f","1f62a","1f62b","1f634",
        "1f60c","1f61b","2764","1f44d","1f62d"
    };

    public void initData(ClientCore clientCore, String myUsername) {
        this.clientCore = clientCore;
        this.myUsername = myUsername;
        
        listAllUsers.setItems(allUsersObs);
        listGroups.setItems(groupsList);
        
        // Cấu hình CellFactory để highlight tin chưa đọc và phân biệt online/offline
        javafx.util.Callback<ListView<String>, ListCell<String>> cellFactory = lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    boolean isUnread = unreadMessages.getOrDefault(item, false);
                    String displayText = item;
                    
                    if (lv == listAllUsers) {
                        if (onlineUsers.contains(item)) {
                            displayText += " (Online)";
                            setTextFill(Color.GREEN);
                        } else {
                            displayText += " (Offline)";
                            setTextFill(Color.GRAY);
                        }
                    } else {
                        setTextFill(Color.BLACK);
                    }
                    
                    if (isUnread) {
                        setText("🔴 " + displayText);
                        setStyle("-fx-font-weight: bold; -fx-background-color: #ffeaa7;");
                    } else {
                        setText(displayText);
                        setStyle("-fx-font-weight: normal; -fx-background-color: transparent;");
                    }
                }
            }
        };
        listAllUsers.setCellFactory(cellFactory);
        listGroups.setCellFactory(cellFactory);
        
        // Cấu hình sự kiện nhận tin nhắn và mất kết nối
        clientCore.setOnMessageReceived(this::handleServerMessage);
        clientCore.setOnDisconnected(() -> {
            javafx.application.Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Đã mất kết nối tới Server!");
                alert.showAndWait();
                handleLogout(); // Hàm handleLogout đã có logic chuyển về ConfigView
            });
        });
        
        // Yêu cầu tải danh sách user
        clientCore.sendMessage(new Message(MessageType.GET_ONLINE_USERS));
        
        // Yêu cầu tải tất cả user trong DB để phục vụ tạo nhóm
        clientCore.sendMessage(new Message(MessageType.GET_ALL_USERS));
        
        // Yêu cầu tải danh sách nhóm mà user này có mặt
        clientCore.sendMessage(new Message(MessageType.GET_GROUPS));
        
        // Yêu cầu tải thông báo tin nhắn chưa đọc
        clientCore.sendMessage(new Message(MessageType.GET_UNREAD_MESSAGES));
        
        // Bắt sự kiện click 1 lần vào user để mở Tab
        listAllUsers.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                String selectedUser = listAllUsers.getSelectionModel().getSelectedItem();
                if (selectedUser != null && !selectedUser.equals(myUsername)) {
                    unreadMessages.put(selectedUser, false);
                    listAllUsers.refresh();
                    openChatTab(selectedUser, false);
                }
            }
        });
        
        // Bắt sự kiện chuyển Tab để xóa trạng thái unread
        tabPaneChats.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) -> {
            if (newTab != null) {
                String title = newTab.getText();
                if (unreadMessages.getOrDefault(title, false)) {
                    unreadMessages.put(title, false);
                    listAllUsers.refresh();
                    listGroups.refresh();
                }
            }
        });
        
        // Bắt sự kiện click vào nhóm
        listGroups.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                String selectedGroup = listGroups.getSelectionModel().getSelectedItem();
                if (selectedGroup != null) {
                    unreadMessages.put(selectedGroup, false);
                    listGroups.refresh();
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
                            if (!allUsersObs.contains(u)) {
                                allUsersObs.add(u);
                            }
                        }
                    }
                    listAllUsers.refresh(); // Cập nhật lại giao diện (chữ Online)
                    break;
                case ALL_USERS_LIST:
                    @SuppressWarnings("unchecked")
                    List<String> dbUsers = (List<String>) msg.getPayload();
                    allUsersObs.clear();
                    for (String u : dbUsers) {
                        if (!u.equals(myUsername)) allUsersObs.add(u);
                    }
                    break;
                case UNREAD_MESSAGES_LIST:
                    @SuppressWarnings("unchecked")
                    List<String> unreadSenders = (List<String>) msg.getPayload();
                    for (String s : unreadSenders) {
                        unreadMessages.put(s, true);
                    }
                    listAllUsers.refresh();
                    listGroups.refresh();
                    break;
                case PRIVATE_CHAT:
                    String sender = msg.getSender();
                    handleIncomingMessage(sender, false);
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
                    handleIncomingMessage(targetGroup, true);
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
                    String tTarget = msg.getReceiver().equals(myUsername) ? msg.getSender() : msg.getReceiver();
                    showTypingIndicator(tTarget, msg.getSender());
                    break;
                case STOP_TYPING:
                    String sTarget = msg.getReceiver().equals(myUsername) ? msg.getSender() : msg.getReceiver();
                    hideTypingIndicator(sTarget);
                    break;
                case GROUP_MEMBERS_RESPONSE:
                    String gName = msg.getReceiver();
                    @SuppressWarnings("unchecked")
                    List<String> currentMembers = (List<String>) msg.getPayload();
                    javafx.application.Platform.runLater(() -> showManageGroupDialog(gName, currentMembers));
                    break;
                case REMOVE_USER_FROM_GROUP:
                    String removedGroup = msg.getContent();
                    groupsList.remove(removedGroup);
                    for (Tab t : tabPaneChats.getTabs()) {
                        if (t.getText().equals(removedGroup)) {
                            javafx.application.Platform.runLater(() -> tabPaneChats.getTabs().remove(t));
                            break;
                        }
                    }
                    break;
                case CHAT_HISTORY_RESPONSE:
                    String target = msg.getReceiver(); // Target của request lịch sử
                    VBox targetChatBox = chatHistories.get(target);
                    if (targetChatBox != null) {
                        targetChatBox.getChildren().clear();
                        @SuppressWarnings("unchecked")
                        List<Message> hist = (List<Message>) msg.getPayload();
                        boolean isGroup = groupsList.contains(target);
                        for (Message hMsg : hist) {
                            boolean isMine = hMsg.getSender().equals(myUsername);
                            int hMsgId = hMsg.getPayload() != null ? (Integer) hMsg.getPayload() : -1;
                            String displayContent = hMsg.getContent();
                            if (isGroup && !isMine) {
                                displayContent = hMsg.getSender() + ": " + displayContent;
                            }
                            addMessageBubble(targetChatBox, displayContent, isMine, hMsgId, target);
                        }
                    }
                    break;
                case SEND_FILE:
                    String targetFile = groupsList.contains(msg.getReceiver()) ? msg.getReceiver() : msg.getSender();
                    handleIncomingMessage(targetFile, groupsList.contains(msg.getReceiver()));
                    VBox boxFile = chatHistories.get(targetFile);
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
                case VOICE_MESSAGE:
                    String targetVoice = groupsList.contains(msg.getReceiver()) ? msg.getReceiver() : msg.getSender();
                    handleIncomingMessage(targetVoice, groupsList.contains(msg.getReceiver()));
                    VBox boxVoice = chatHistories.get(targetVoice);
                    if (boxVoice != null) {
                        byte[] voiceBytes = (byte[]) msg.getPayload();
                        Button btnPlay = new Button("▶ Nghe tin nhắn thoại");
                        btnPlay.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-radius: 15;");
                        btnPlay.setOnAction(e -> {
                            com.chatapp.client.media.AudioPlayer.play(voiceBytes);
                        });
                        HBox rowVoice = new HBox(btnPlay);
                        rowVoice.setAlignment(Pos.CENTER_LEFT);
                        boxVoice.getChildren().add(rowVoice);
                    }
                    break;
                case WEBCAM_FRAME:
                    String targetCam = groupsList.contains(msg.getReceiver()) ? msg.getReceiver() : msg.getSender();
                    handleIncomingMessage(targetCam, groupsList.contains(msg.getReceiver()));
                    VBox boxCam = chatHistories.get(targetCam);
                    if (boxCam != null) {
                        Object payload = msg.getPayload();
                        boolean isMine = msg.getSender().equals(myUsername);
                        javafx.application.Platform.runLater(() -> {
                            if (payload instanceof byte[]) {
                                renderPhotoBubble((byte[]) payload, boxCam, isMine, msg.getSender());
                            } else if (payload instanceof java.util.List) {
                                renderVideoBubble((java.util.List<byte[]>) payload, boxCam, isMine, msg.getSender());
                            }
                        });
                    }
                    break;
                default:
                    break;
            }
        });
    }

    private void handleIncomingMessage(String target, boolean isGroup) {
        boolean isTabSelected = false;
        Tab selectedTab = tabPaneChats.getSelectionModel().getSelectedItem();
        if (selectedTab != null && selectedTab.getText().equals(target)) {
            isTabSelected = true;
        }
        
        if (!isTabSelected) {
            unreadMessages.put(target, true);
            listAllUsers.refresh();
            listGroups.refresh();
        }
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
        
        com.chatapp.client.richtext.EmojiTextArea txtInput = new com.chatapp.client.richtext.EmojiTextArea();
        txtInput.setPrefHeight(45);
        txtInput.setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-control-inner-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent; -fx-background-insets: 0; -fx-padding: 5; -fx-font-size: 14px; -fx-font-family: 'Segoe UI', sans-serif; -fx-text-fill: black;");
        
        javafx.stage.Popup emojiPopup = new javafx.stage.Popup();
        emojiPopup.setAutoHide(true);
        javafx.scene.layout.FlowPane emojiGrid = new javafx.scene.layout.FlowPane();
        emojiGrid.setPrefWrapLength(240);
        emojiGrid.setVgap(5); emojiGrid.setHgap(5);
        emojiGrid.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-border-color: #ccc; -fx-border-radius: 5; -fx-background-radius: 5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 0);");
        
        for(String hex : ALL_EMOJIS) {
            String iconPath = "/com/chatapp/client/emojis/" + hex + ".png";
            try {
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(new javafx.scene.image.Image(getClass().getResourceAsStream(iconPath)));
                iv.setFitWidth(24); iv.setFitHeight(24);
                Button btn = new Button();
                btn.setGraphic(iv);
                btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2;");
                btn.setOnAction(ev -> {
                    txtInput.insertEmoji(iconPath);
                    txtInput.requestFocus();
                    emojiPopup.hide();
                });
                emojiGrid.getChildren().add(btn);
            } catch(Exception e) {}
        }
        emojiPopup.getContent().add(emojiGrid);

        Button btnEmojiToggle = new Button();
        try {
            javafx.scene.image.ImageView ivToggle = new javafx.scene.image.ImageView(new javafx.scene.image.Image(getClass().getResourceAsStream("/com/chatapp/client/emojis/1f600.png")));
            ivToggle.setFitWidth(22); ivToggle.setFitHeight(22);
            btnEmojiToggle.setGraphic(ivToggle);
        } catch(Exception e) {
            btnEmojiToggle.setText("😊");
            btnEmojiToggle.setStyle("-fx-font-family: 'Segoe UI Emoji';");
        }
        btnEmojiToggle.setStyle(btnEmojiToggle.getStyle() + "-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 20px; -fx-padding: 5;");
        btnEmojiToggle.setMinSize(Button.USE_PREF_SIZE, Button.USE_PREF_SIZE); // Không cho nút bị bóp nhỏ thành 3 chấm
        btnEmojiToggle.setOnAction(ev -> {
            if (emojiPopup.isShowing()) {
                emojiPopup.hide();
            } else {
                javafx.geometry.Bounds bounds = btnEmojiToggle.localToScreen(btnEmojiToggle.getBoundsInLocal());
                emojiPopup.show(btnEmojiToggle, bounds.getMinX() - 200, bounds.getMinY() - 250);
            }
        });
        
        // Tính năng Đang soạn tin... (Cho cả chat 1-1 và Group)
        txtInput.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.isEmpty() && oldText.isEmpty()) {
                clientCore.sendMessage(new Message(MessageType.TYPING, myUsername, target, ""));
            } else if (newText.isEmpty() && !oldText.isEmpty()) {
                clientCore.sendMessage(new Message(MessageType.STOP_TYPING, myUsername, target, ""));
            }
        });
        
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
        
        Button btnSend = new Button("➤");
        btnSend.setTooltip(new Tooltip("Gửi tin nhắn"));
        btnSend.setStyle("-fx-background-color: transparent; -fx-text-fill: #0084ff; -fx-font-size: 24px; -fx-cursor: hand; -fx-padding: 0 10 5 0;");
        btnSend.setMinSize(Button.USE_PREF_SIZE, Button.USE_PREF_SIZE);
        btnSend.setOnAction(ev -> sendChatMessage(target, txtInput, chatBox, isGroup));
        
        // Box giả làm thanh nhập chữ chứa khung Text và nút Emoji bên trong
        HBox pseudoInputBox = new HBox(5, txtInput, btnEmojiToggle);
        pseudoInputBox.setAlignment(Pos.BOTTOM_RIGHT);
        pseudoInputBox.setStyle("-fx-background-color: #f0f2f5; -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #ccd0d5; -fx-padding: 2;");
        HBox.setHgrow(txtInput, Priority.ALWAYS);
        HBox.setHgrow(pseudoInputBox, Priority.ALWAYS);
        
        Button btnFile = new Button("📎");
        btnFile.setTooltip(new Tooltip("Gửi File"));
        btnFile.setStyle("-fx-background-color: transparent; -fx-text-fill: #2ecc71; -fx-font-size: 24px; -fx-cursor: hand; -fx-padding: 0 5 5 5;");
        btnFile.setMinSize(Button.USE_PREF_SIZE, Button.USE_PREF_SIZE);
        btnFile.setOnAction(ev -> {
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
        
        Button btnMic = new Button("🎤");
        btnMic.setTooltip(new Tooltip("Nhấn giữ để ghi âm"));
        btnMic.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-font-size: 24px; -fx-cursor: hand; -fx-padding: 0 5 5 5;");
        btnMic.setMinSize(Button.USE_PREF_SIZE, Button.USE_PREF_SIZE);
        
        com.chatapp.client.media.AudioRecorder[] recorder = new com.chatapp.client.media.AudioRecorder[1];
        
        btnMic.setOnMousePressed(ev -> {
            btnMic.setStyle("-fx-background-color: #ffeaea; -fx-background-radius: 20; -fx-text-fill: #e74c3c; -fx-font-size: 24px; -fx-cursor: hand; -fx-padding: 0 5 5 5;");
            recorder[0] = new com.chatapp.client.media.AudioRecorder();
            try {
                recorder[0].startRecording();
            } catch(Exception e) {
                e.printStackTrace();
            }
        });
        
        btnMic.setOnMouseReleased(ev -> {
            btnMic.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-font-size: 24px; -fx-cursor: hand; -fx-padding: 0 5 5 5;");
            if (recorder[0] != null) {
                byte[] audioData = recorder[0].stopRecording();
                if (audioData != null && audioData.length > 0) {
                    Message voiceMsg = new Message(MessageType.VOICE_MESSAGE, myUsername, target, "Tin nhắn thoại");
                    voiceMsg.setPayload(audioData);
                    clientCore.sendMessage(voiceMsg);
                    
                    // Hiện bong bóng Chat Audio của mình
                    Button btnPlay = new Button("▶ Nghe tin nhắn thoại");
                    btnPlay.setStyle("-fx-background-color: #0084ff; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-radius: 15; -fx-padding: 5 10;");
                    btnPlay.setOnAction(e -> {
                        com.chatapp.client.media.AudioPlayer.play(audioData);
                    });
                    HBox rowVoice = new HBox(btnPlay);
                    rowVoice.setAlignment(Pos.CENTER_RIGHT);
                    rowVoice.setPadding(new javafx.geometry.Insets(5, 5, 5, 50));
                    chatBox.getChildren().add(rowVoice);
                }
            }
        });
        
        Button btnCam = new Button("📸");
        btnCam.setTooltip(new Tooltip("Camera - Chụp ảnh/Quay video"));
        btnCam.setStyle("-fx-background-color: transparent; -fx-text-fill: #9b59b6; -fx-font-size: 24px; -fx-cursor: hand; -fx-padding: 0 5 5 5;");
        btnCam.setMinSize(Button.USE_PREF_SIZE, Button.USE_PREF_SIZE);
        
        btnCam.setOnAction(ev -> {
            new com.chatapp.client.media.WebcamDialog().show(media -> {
                if (media instanceof byte[]) {
                    byte[] photoBytes = (byte[]) media;
                    if (photoBytes.length > 0) {
                        Message camMsg = new Message(MessageType.WEBCAM_FRAME, myUsername, target, "Đã gửi một ảnh");
                        camMsg.setPayload(photoBytes);
                        clientCore.sendMessage(camMsg);
                        javafx.application.Platform.runLater(() -> renderPhotoBubble(photoBytes, chatBox, true, myUsername));
                    }
                } else if (media instanceof java.util.List) {
                    @SuppressWarnings("unchecked")
                    java.util.List<byte[]> videoFrames = (java.util.List<byte[]>) media;
                    if (!videoFrames.isEmpty()) {
                        Message camMsg = new Message(MessageType.WEBCAM_FRAME, myUsername, target, "Đã gửi một video");
                        camMsg.setPayload(videoFrames);
                        clientCore.sendMessage(camMsg);
                        javafx.application.Platform.runLater(() -> renderVideoBubble(videoFrames, chatBox, true, myUsername));
                    }
                }
            });
        });
        
        HBox bottomBar = new HBox(5);
        bottomBar.setAlignment(Pos.BOTTOM_LEFT);
        
        if (isGroup) {
            Button btnManageGroup = new Button("⚙");
            btnManageGroup.setTooltip(new Tooltip("Quản lý nhóm"));
            btnManageGroup.setStyle("-fx-background-color: transparent; -fx-text-fill: #9b59b6; -fx-font-size: 26px; -fx-cursor: hand; -fx-padding: 0 5 5 5;");
            btnManageGroup.setMinSize(Button.USE_PREF_SIZE, Button.USE_PREF_SIZE);
            btnManageGroup.setOnAction(ev -> {
                clientCore.sendMessage(new Message(MessageType.GET_GROUP_MEMBERS, myUsername, target, ""));
            });
            bottomBar.getChildren().add(btnManageGroup);
        }
        bottomBar.getChildren().addAll(btnMic, btnCam, btnFile, pseudoInputBox, btnSend);
        
        root.getChildren().addAll(scrollPane, bottomBar);
        tab.setContent(root);
        
        // Quản lý dọn dẹp khi đóng tab
        tab.setOnClosed(e -> chatHistories.remove(target));
        chatHistories.put(target, chatBox);
        
        tabPaneChats.getTabs().add(tab);
        tabPaneChats.getSelectionModel().select(tab);
        
        // Gửi yêu cầu lấy lịch sử chat cho cả chat riêng và group
        clientCore.sendMessage(new Message(MessageType.GET_CHAT_HISTORY, myUsername, target, ""));
    }
    
    private void renderPhotoBubble(byte[] photoBytes, VBox chatBox, boolean isMine, String senderName) {
        try {
            java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(photoBytes);
            javafx.scene.image.Image img = new javafx.scene.image.Image(bis);
            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
            iv.setFitWidth(200);
            iv.setPreserveRatio(true);
            iv.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 0); -fx-cursor: hand;");
            
            iv.setOnMouseClicked(e -> showMediaViewer(photoBytes, null));
            
            VBox wrapper = new VBox(2);
            wrapper.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            if (!isMine && senderName != null && !senderName.isEmpty()) {
                javafx.scene.control.Label lblName = new javafx.scene.control.Label(senderName);
                lblName.setStyle("-fx-text-fill: gray; -fx-font-size: 10px;");
                wrapper.getChildren().add(lblName);
            }
            wrapper.getChildren().add(iv);
            
            HBox rowImg = new HBox(wrapper);
            rowImg.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            if (isMine) rowImg.setPadding(new javafx.geometry.Insets(5, 5, 5, 50));
            chatBox.getChildren().add(rowImg);
        } catch(Exception e) {}
    }

    private void renderVideoBubble(java.util.List<byte[]> frames, VBox chatBox, boolean isMine, String senderName) {
        if (frames == null || frames.isEmpty()) return;
        try {
            // Lấy frame đầu tiên làm thumbnail
            java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(frames.get(0));
            javafx.scene.image.Image img = new javafx.scene.image.Image(bis);
            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
            iv.setFitWidth(200);
            iv.setPreserveRatio(true);
            iv.setStyle("-fx-cursor: hand;");
            iv.setOnMouseClicked(e -> showMediaViewer(null, frames));
            
            javafx.scene.layout.StackPane sp = new javafx.scene.layout.StackPane(iv);
            Button btnPlay = new Button("▶");
            btnPlay.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-text-fill: white; -fx-font-size: 30px; -fx-background-radius: 25; -fx-cursor: hand;");
            sp.getChildren().add(btnPlay);
            sp.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 0);");
            
            btnPlay.setOnAction(e -> {
                btnPlay.setVisible(false);
                Thread playThread = new Thread(() -> {
                    for (byte[] frameBytes : frames) {
                        try {
                            java.io.ByteArrayInputStream fBis = new java.io.ByteArrayInputStream(frameBytes);
                            javafx.scene.image.Image fImg = new javafx.scene.image.Image(fBis);
                            javafx.application.Platform.runLater(() -> iv.setImage(fImg));
                            Thread.sleep(100);
                        } catch (Exception ex) {}
                    }
                    javafx.application.Platform.runLater(() -> {
                        iv.setImage(img);
                        btnPlay.setVisible(true);
                    });
                });
                playThread.start();
            });
            
            VBox wrapper = new VBox(2);
            wrapper.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            if (!isMine && senderName != null && !senderName.isEmpty()) {
                javafx.scene.control.Label lblName = new javafx.scene.control.Label(senderName);
                lblName.setStyle("-fx-text-fill: gray; -fx-font-size: 10px;");
                wrapper.getChildren().add(lblName);
            }
            wrapper.getChildren().add(sp);
            
            HBox rowImg = new HBox(wrapper);
            rowImg.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            if (isMine) rowImg.setPadding(new javafx.geometry.Insets(5, 5, 5, 50));
            chatBox.getChildren().add(rowImg);
        } catch(Exception e) {}
    }

    private void showMediaViewer(byte[] photoBytes, java.util.List<byte[]> videoFrames) {
        javafx.stage.Stage stage = new javafx.stage.Stage();
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        
        javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView();
        iv.setFitWidth(800);
        iv.setFitHeight(600);
        iv.setPreserveRatio(true);
        
        VBox root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: rgba(0,0,0,0.9);");
        
        if (photoBytes != null) {
            stage.setTitle("Xem Ảnh Toàn Màn Hình");
            try {
                iv.setImage(new javafx.scene.image.Image(new java.io.ByteArrayInputStream(photoBytes)));
                root.getChildren().add(iv);
            } catch (Exception e) {}
        } else if (videoFrames != null && !videoFrames.isEmpty()) {
            stage.setTitle("Xem Video Toàn Màn Hình");
            try {
                iv.setImage(new javafx.scene.image.Image(new java.io.ByteArrayInputStream(videoFrames.get(0))));
                
                Button btnPlay = new Button("▶");
                btnPlay.setStyle("-fx-background-color: rgba(255,255,255,0.3); -fx-text-fill: white; -fx-font-size: 50px; -fx-background-radius: 50; -fx-cursor: hand;");
                
                javafx.scene.layout.StackPane sp = new javafx.scene.layout.StackPane(iv, btnPlay);
                root.getChildren().add(sp);
                
                btnPlay.setOnAction(e -> {
                    btnPlay.setVisible(false);
                    new Thread(() -> {
                        for (byte[] frame : videoFrames) {
                            try {
                                javafx.scene.image.Image fImg = new javafx.scene.image.Image(new java.io.ByteArrayInputStream(frame));
                                javafx.application.Platform.runLater(() -> iv.setImage(fImg));
                                Thread.sleep(100);
                            } catch (Exception ex) {}
                        }
                        javafx.application.Platform.runLater(() -> btnPlay.setVisible(true));
                    }).start();
                });
            } catch (Exception e) {}
        }
        
        stage.setScene(new Scene(root, 850, 650));
        stage.show();
    }
    
    private void sendChatMessage(String target, com.chatapp.client.richtext.EmojiTextArea txtInput, VBox chatBox, boolean isGroup) {
        String content = txtInput.getPlainTextWithEmojis();
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
        
        // Regex nhận diện động từ mảng ALL_EMOJIS
        StringBuilder regexBuilder = new StringBuilder("(");
        for (int i=0; i<ALL_EMOJIS.length; i++) {
           if(ALL_EMOJIS[i].equals("2764")) regexBuilder.append("❤️");
           else regexBuilder.append(new String(Character.toChars(Integer.parseInt(ALL_EMOJIS[i], 16))));
           if(i < ALL_EMOJIS.length - 1) regexBuilder.append("|");
        }
        regexBuilder.append(")");
        Pattern pattern = Pattern.compile(regexBuilder.toString());
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
            String emojiStr = matcher.group();
            String iconPath = "";
            for (String hex : ALL_EMOJIS) {
                String eChar = hex.equals("2764") ? "❤️" : new String(Character.toChars(Integer.parseInt(hex, 16)));
                if (eChar.equals(emojiStr)) {
                    iconPath = "/com/chatapp/client/emojis/" + hex + ".png";
                    break;
                }
            }
            try {
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(new javafx.scene.image.Image(getClass().getResourceAsStream(iconPath)));
                iv.setFitWidth(20);
                iv.setFitHeight(20);
                
                // Thủ thuật nhỏ: TextFlow cần một text rỗng để layout ImageView cho đẹp
                Text space = new Text("\u200B"); // Zero-width space
                textFlow.getChildren().addAll(iv, space);
            } catch (Exception ex) {
                // Fallback nếu thiếu ảnh
                Text emojiText = new Text(emojiStr);
                emojiText.setStyle("-fx-font-family: 'Segoe UI Emoji', 'Apple Color Emoji'; -fx-font-size: 18px;");
                textFlow.getChildren().add(emojiText);
            }
            
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
    
    private void showTypingIndicator(String tabName, String typingUser) {
        VBox history = chatHistories.get(tabName);
        if (history != null && !typingNodes.containsKey(tabName)) {
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            Label lbl = new Label("💬 " + typingUser + " đang soạn tin...");
            lbl.setStyle("-fx-font-style: italic; -fx-text-fill: gray;");
            row.getChildren().add(lbl);
            
            row.setOpacity(0.0);
            history.getChildren().add(row);
            typingNodes.put(tabName, row);
            
            FadeTransition ft = new FadeTransition(Duration.millis(300), row);
            ft.setToValue(1.0);
            ft.play();
        }
    }

    private void hideTypingIndicator(String tabName) {
        VBox history = chatHistories.get(tabName);
        Node typingNode = typingNodes.remove(tabName);
        if (history != null && typingNode != null) {
            FadeTransition ft = new FadeTransition(Duration.millis(300), typingNode);
            ft.setToValue(0.0);
            ft.setOnFinished(e -> history.getChildren().remove(typingNode));
            ft.play();
        }
    }

    private void showManageGroupDialog(String groupName, List<String> currentMembers) {
        boolean isAdmin = !currentMembers.isEmpty() && currentMembers.get(0).equals(myUsername);
        String adminName = currentMembers.isEmpty() ? "Unknown" : currentMembers.get(0);
        
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Quản lý nhóm: " + groupName);
        dialog.setHeaderText("Trưởng nhóm: " + adminName + (isAdmin ? " (Là bạn)" : ""));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        TabPane tabPane = new TabPane();
        
        // Tab 1: Xem danh sách
        Tab tabView = new Tab("Danh sách");
        tabView.setClosable(false);
        ListView<String> lvMembers = new ListView<>();
        lvMembers.getItems().addAll(currentMembers);
        tabView.setContent(lvMembers);
        
        // Tab 2: Thêm thành viên
        Tab tabAdd = new Tab("Thêm thành viên");
        tabAdd.setClosable(false);
        VBox boxAdd = new VBox(5);
        boxAdd.setStyle("-fx-padding: 10;");
        ListView<CheckBox> lvAdd = new ListView<>();
        for (String u : allUsersObs) {
            if (!currentMembers.contains(u)) {
                lvAdd.getItems().add(new CheckBox(u));
            }
        }
        Button btnAdd = new Button("Thêm vào nhóm");
        btnAdd.setOnAction(e -> {
            List<String> toAdd = new java.util.ArrayList<>();
            for (CheckBox cb : lvAdd.getItems()) {
                if (cb.isSelected()) toAdd.add(cb.getText());
            }
            if (!toAdd.isEmpty()) {
                List<String> newMems = new java.util.ArrayList<>(currentMembers);
                newMems.addAll(toAdd);
                Message msg = new Message(MessageType.UPDATE_GROUP_MEMBERS, myUsername, groupName, "");
                msg.setPayload(newMems);
                clientCore.sendMessage(msg);
                dialog.close();
            }
        });
        boxAdd.getChildren().addAll(new Label("Chọn người muốn thêm:"), lvAdd, btnAdd);
        tabAdd.setContent(boxAdd);
        
        // Tab 3: Xóa thành viên (Chỉ Admin)
        Tab tabRemove = new Tab("Kick thành viên");
        tabRemove.setClosable(false);
        VBox boxRem = new VBox(5);
        boxRem.setStyle("-fx-padding: 10;");
        if (isAdmin) {
            ListView<CheckBox> lvRem = new ListView<>();
            for (String u : currentMembers) {
                if (!u.equals(myUsername)) {
                    lvRem.getItems().add(new CheckBox(u));
                }
            }
            Button btnRem = new Button("Kick khỏi nhóm");
            btnRem.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
            btnRem.setOnAction(e -> {
                List<String> toRem = new java.util.ArrayList<>();
                for (CheckBox cb : lvRem.getItems()) {
                    if (cb.isSelected()) toRem.add(cb.getText());
                }
                if (!toRem.isEmpty()) {
                    List<String> newMems = new java.util.ArrayList<>(currentMembers);
                    newMems.removeAll(toRem);
                    Message msg = new Message(MessageType.UPDATE_GROUP_MEMBERS, myUsername, groupName, "");
                    msg.setPayload(newMems);
                    clientCore.sendMessage(msg);
                    dialog.close();
                }
            });
            boxRem.getChildren().addAll(new Label("Chọn người muốn kick:"), lvRem, btnRem);
        } else {
            boxRem.getChildren().add(new Label("Chỉ Trưởng Nhóm (" + adminName + ") mới có quyền kick thành viên!"));
        }
        tabRemove.setContent(boxRem);
        
        // Tab 4: Cài đặt (Chỉ Admin)
        Tab tabSettings = new Tab("Cài đặt");
        tabSettings.setClosable(false);
        VBox boxSet = new VBox(10);
        boxSet.setStyle("-fx-padding: 10; -fx-alignment: center;");
        if (isAdmin) {
            Button btnDisband = new Button("Giải tán nhóm");
            btnDisband.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
            btnDisband.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Bạn có chắc chắn muốn giải tán nhóm này vĩnh viễn?", ButtonType.YES, ButtonType.NO);
                confirm.showAndWait().ifPresent(res -> {
                    if (res == ButtonType.YES) {
                        clientCore.sendMessage(new Message(MessageType.DISBAND_GROUP, myUsername, groupName, ""));
                        dialog.close();
                    }
                });
            });
            boxSet.getChildren().addAll(new Label("Khu vực nguy hiểm:"), btnDisband);
        } else {
            boxSet.getChildren().add(new Label("Chỉ Trưởng Nhóm (" + adminName + ") mới có thể giải tán nhóm!"));
        }
        tabSettings.setContent(boxSet);
        
        tabPane.getTabs().addAll(tabView, tabAdd, tabRemove, tabSettings);
        dialog.getDialogPane().setContent(tabPane);
        dialog.showAndWait();
    }

    @FXML
    private void handleCreateGroup() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Tạo Nhóm Mới");
        dialog.setHeaderText("Đặt tên nhóm và chọn thành viên\n(Bạn là Trưởng Nhóm)");

        ButtonType createButtonType = new ButtonType("Tạo", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        VBox content = new VBox(10);
        TextField txtGroupName = new TextField();
        txtGroupName.setPromptText("Nhập tên nhóm... (Bắt buộc)");
        
        ListView<CheckBox> userChecks = new ListView<>();
        for (String user : allUsersObs) {
            if (!user.equals(myUsername)) {
                CheckBox cb = new CheckBox(user);
                userChecks.getItems().add(cb);
            }
        }
        
        content.getChildren().addAll(new Label("Tên nhóm:"), txtGroupName, new Label("Thành viên:"), userChecks);
        dialog.getDialogPane().setContent(content);

        // Vô hiệu hóa nút Tạo nếu chưa nhập tên nhóm
        Node createButton = dialog.getDialogPane().lookupButton(createButtonType);
        createButton.setDisable(true);
        txtGroupName.textProperty().addListener((observable, oldValue, newValue) -> {
            createButton.setDisable(newValue.trim().isEmpty());
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                return txtGroupName.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(groupName -> {
            if (!groupName.trim().isEmpty()) {
                List<String> members = new java.util.ArrayList<>();
                members.add(myUsername); // Người tạo được thêm đầu tiên = ADMIN
                for (CheckBox cb : userChecks.getItems()) {
                    if (cb.isSelected()) members.add(cb.getText());
                }
                if (members.size() > 1) {
                    Message msg = new Message(MessageType.CREATE_GROUP, myUsername, "", groupName.trim());
                    msg.setPayload(members);
                    clientCore.sendMessage(msg);
                } else {
                    Alert a = new Alert(Alert.AlertType.WARNING, "Nhóm phải có ít nhất 2 thành viên!");
                    a.setHeaderText(null);
                    a.show();
                }
            }
        });
    }

    @FXML
    private void handleLogout() {
        if (clientCore != null) {
            clientCore.sendMessage(new Message(MessageType.LOGOUT));
            clientCore.closeConnection();
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chatapp/client/ConfigView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) listAllUsers.getScene().getWindow();
            stage.setTitle("Quản Lý Máy Chủ - Chat App");
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
