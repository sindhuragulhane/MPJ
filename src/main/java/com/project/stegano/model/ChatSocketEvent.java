package com.project.stegano.model;

import java.util.List;

public record ChatSocketEvent(
        String type,
        String currentUser,
        String roomId,
        ChatRoom room,
        List<ChatRoom> rooms,
        ChatMessage message,
        List<ChatMessage> messages,
        List<String> onlineUsers,
        String error
) {
    public static ChatSocketEvent bootstrap(String currentUser, List<ChatRoom> rooms, List<String> onlineUsers) {
        return new ChatSocketEvent("bootstrap", currentUser, null, null, rooms, null, null, onlineUsers, null);
    }

    public static ChatSocketEvent roomHistory(String roomId, List<ChatMessage> messages) {
        return new ChatSocketEvent("room_history", null, roomId, null, null, null, messages, null, null);
    }

    public static ChatSocketEvent roomCreated(ChatRoom room) {
        return new ChatSocketEvent("room_created", null, room.id(), room, null, null, null, null, null);
    }

    public static ChatSocketEvent message(ChatMessage message) {
        return new ChatSocketEvent("message", null, message.roomId(), null, null, message, null, null, null);
    }

    public static ChatSocketEvent presence(List<String> onlineUsers) {
        return new ChatSocketEvent("presence", null, null, null, null, null, null, onlineUsers, null);
    }

    public static ChatSocketEvent authRequired() {
        return new ChatSocketEvent("auth_required", null, null, null, null, null, null, null, null);
    }

    public static ChatSocketEvent error(String error) {
        return new ChatSocketEvent("error", null, null, null, null, null, null, null, error);
    }
}
