package com.chatapp.client.richtext;

import javafx.scene.Node;
import javafx.scene.layout.Region;

public class EmptyLinkedImage implements LinkedImage {
    @Override public boolean isReal() { return false; }
    @Override public String getImagePath() { return ""; }
    @Override public Node createNode() { return new Region(); }
}
