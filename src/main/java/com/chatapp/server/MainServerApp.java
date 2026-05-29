package com.chatapp.server;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainServerApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Khởi tạo Database ngay khi bật Server
        com.chatapp.server.db.SQLiteConnection.initialize();
        
        // Tải giao diện FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chatapp/server/ServerView.fxml"));
        Parent root = loader.load();
        
        Scene scene = new Scene(root, 800, 500);
        // Tải file CSS để làm đẹp
        scene.getStylesheets().add(getClass().getResource("/com/chatapp/server/styles.css").toExternalForm());
        
        primaryStage.setTitle("Chat Server Management Panel");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }
    
    @Override
    public void stop() throws Exception {
        super.stop();
        // Đảm bảo đóng server khi người dùng ấn tắt cửa sổ
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
