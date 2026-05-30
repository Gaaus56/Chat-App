package com.chatapp.client.richtext;

import javafx.scene.Node;

public interface LinkedImage {
    boolean isReal();
    String getImagePath();
    Node createNode();
}
