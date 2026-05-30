package com.chatapp.client.richtext;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class RealLinkedImage implements LinkedImage {
    private final String imagePath;
    
    public RealLinkedImage(String imagePath) {
        this.imagePath = imagePath;
    }
    
    @Override public boolean isReal() { return true; }
    @Override public String getImagePath() { return imagePath; }
    
    @Override public Node createNode() {
        try {
            ImageView iv = new ImageView(new Image(getClass().getResourceAsStream(imagePath)));
            iv.setFitWidth(20);
            iv.setFitHeight(20);
            return iv;
        } catch (Exception e) {
            return new javafx.scene.text.Text("?");
        }
    }
}
