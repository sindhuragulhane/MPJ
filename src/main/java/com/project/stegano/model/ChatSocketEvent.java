package com.project.stegano.model;

import java.util.List;

public record ChatSocketEvent(
        String type,
        ChatMessage message,
        List<ChatMessage> messages,
        String error
) {
    public static ChatSocketEvent history(List<ChatMessage> messages) {
        return new ChatSocketEvent("history", null, messages, null);
    }

    public static ChatSocketEvent message(ChatMessage message) {
        return new ChatSocketEvent("message", message, null, null);
    }

    public static ChatSocketEvent error(String error) {
        return new ChatSocketEvent("error", null, null, error);
    }
}
