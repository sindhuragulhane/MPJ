package com.project.stegano.service;

import com.project.stegano.model.ChatMessage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class MessageStore {

    private final List<ChatMessage> messages = new CopyOnWriteArrayList<>();

    public void add(ChatMessage message) {
        messages.add(message);
    }

    public List<ChatMessage> snapshot() {
        return List.copyOf(messages);
    }

}
