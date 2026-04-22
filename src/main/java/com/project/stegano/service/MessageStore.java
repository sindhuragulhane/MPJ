package com.project.stegano.service;

import com.project.stegano.model.ChatMessage;
import com.project.stegano.model.ChatRoom;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class MessageStore {

    private final Map<String, ChatRoom> rooms = new ConcurrentHashMap<>();
    private final Map<String, List<ChatMessage>> roomMessages = new ConcurrentHashMap<>();

    public void saveRoom(ChatRoom room) {
        rooms.put(room.id(), room);
        roomMessages.computeIfAbsent(room.id(), ignored -> new CopyOnWriteArrayList<>());
    }

    public ChatRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public List<ChatRoom> roomsForUser(String username) {
        return rooms.values().stream()
                .filter(room -> room.participants().contains(username))
                .sorted(Comparator.comparing(ChatRoom::updatedAt).reversed())
                .toList();
    }

    public void addMessage(ChatMessage message) {
        roomMessages.computeIfAbsent(message.roomId(), ignored -> new CopyOnWriteArrayList<>()).add(message);
    }

    public List<ChatMessage> messagesForRoom(String roomId) {
        return List.copyOf(roomMessages.getOrDefault(roomId, List.of()));
    }
}
