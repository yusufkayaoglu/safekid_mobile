package com.safekid.mobile;

public class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_AI   = 1;

    public final String text;
    public final int type;

    public ChatMessage(String text, int type) {
        this.text = text;
        this.type = type;
    }
}
