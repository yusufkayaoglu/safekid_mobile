package com.safekid.mobile.network.dto;

public class AiChatRequest {
    public String cocukUniqueId;
    public String message;

    public AiChatRequest(String cocukUniqueId, String message) {
        this.cocukUniqueId = cocukUniqueId;
        this.message = message;
    }
}
