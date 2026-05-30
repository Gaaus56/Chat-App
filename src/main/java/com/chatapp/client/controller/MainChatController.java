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
    
    // Lưu trữ giao diện hiển thị Webcam
    private Map<String, javafx.stage.Stage> videoStages = new HashMap<>();
    private Map<String, javafx.scene.image.ImageView> videoViews = new HashMap<>();

    
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
                case VOICE_MESSAGE:
                    String senderVoice = msg.getSender();
                    openChatTab(senderVoice, false);
                    VBox boxVoice = chatHistories.get(senderVoice);
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
                    String senderCam = msg.getSender();
                    javafx.application.Platform.runLater(() -> {
                        showVideoStage(senderCam);
                        byte[] frameBytes = (byte[]) msg.getPayload();
                        try {
                            java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(frameBytes);
                            javafx.scene.image.Image img = new javafx.scene.image.Image(bis);
                            videoViews.get(senderCam).setImage(img);
                        } catch (Exception ex) {}
                    });
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
        
        Button btnCam = new Button("📹");
        btnCam.setTooltip(new Tooltip("Bật/Tắt Webcam"));
        btnCam.setStyle("-fx-background-color: transparent; -fx-text-fill: #9b59b6; -fx-font-size: 24px; -fx-cursor: hand; -fx-padding: 0 5 5 5;");
        btnCam.setMinSize(Button.USE_PREF_SIZE, Button.USE_PREF_SIZE);
        
        com.chatapp.client.media.WebcamService[] webcamService = new com.chatapp.client.media.WebcamService[1];
        btnCam.setOnAction(ev -> {
            if (webcamService[0] == null) {
                webcamService[0] = new com.chatapp.client.media.WebcamService();
                webcamService[0].startStreaming(clientCore, myUsername, target);
                btnCam.setStyle("-fx-background-color: #ffeaea; -fx-text-fill: #9b59b6; -fx-font-size: 24px; -fx-cursor: hand; -fx-padding: 0 5 5 5;");
            } else {
                webcamService[0].stopStreaming();
                webcamService[0] = null;
                btnCam.setStyle("-fx-background-color: transparent; -fx-text-fill: #9b59b6; -fx-font-size: 24px; -fx-cursor: hand; -fx-padding: 0 5 5 5;");
            }
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
        
        // Gửi yêu cầu lấy lịch sử chat (chỉ cho chat riêng)
        if (!isGroup) {
            clientCore.sendMessage(new Message(MessageType.GET_CHAT_HISTORY, myUsername, target, ""));
        }
    }

    private void showVideoStage(String sender) {
        if (!videoStages.containsKey(sender)) {
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Video call với " + sender);
            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView();
            iv.setFitWidth(640);
            iv.setFitHeight(480);
            videoViews.put(sender, iv);
            
            VBox box = new VBox(iv);
            box.setAlignment(Pos.CENTER);
            box.setStyle("-fx-background-color: black;");
            stage.setScene(new Scene(box, 640, 480));
            stage.setOnCloseRequest(e -> {
                videoStages.remove(sender);
                videoViews.remove(sender);
            });
            videoStages.put(sender, stage);
            stage.show();
        }
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
        Dialog<List<String>> dialog = new Dialog<>();
        dialog.setTitle("Quản lý nhóm: " + groupName);
        dialog.setHeaderText("Thêm hoặc xóa thành viên");

        ButtonType saveButtonType = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        VBox content = new VBox(10);
        ListView<CheckBox> userChecks = new ListView<>();
        
        for (String user : onlineUsers) {
            if (user.equals(myUsername)) continue;
            CheckBox cb = new CheckBox(user);
            if (currentMembers.contains(user)) {
                cb.setSelected(true);
            }
            userChecks.getItems().add(cb);
        }
        
        content.getChildren().addAll(new Label("Danh sách thành viên:"), userChecks);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                List<String> newMembers = new java.util.ArrayList<>();
                newMembers.add(myUsername);
                for (CheckBox cb : userChecks.getItems()) {
                    if (cb.isSelected()) newMembers.add(cb.getText());
                }
                return newMembers;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newMembers -> {
            Message msg = new Message(MessageType.UPDATE_GROUP_MEMBERS, myUsername, groupName, "");
            msg.setPayload(newMembers);
            clientCore.sendMessage(msg);
        });
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
