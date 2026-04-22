package com.project.stegano.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.stegano.model.ChatMessage;
import com.project.stegano.model.ChatSocketEvent;
import com.project.stegano.model.SendMessageRequest;
import com.project.stegano.service.ChatService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper;
    private final ChatService chatService;

    public ChatWebSocketHandler(ObjectMapper objectMapper, ChatService chatService) {
        this.objectMapper = objectMapper;
        this.chatService = chatService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        sendEvent(session, ChatSocketEvent.history(chatService.getMessages()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            SendMessageRequest request = objectMapper.readValue(message.getPayload(), SendMessageRequest.class);
            ChatMessage savedMessage = chatService.sendMessage(request.user(), request.message());
            broadcast(ChatSocketEvent.message(savedMessage));
        } catch (IllegalArgumentException e) {
            sendEvent(session, ChatSocketEvent.error(e.getMessage()));
        } catch (Exception e) {
            sendEvent(session, ChatSocketEvent.error("Unable to send message"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    private void broadcast(ChatSocketEvent event) {
        sessions.removeIf(session -> !session.isOpen());
        sessions.forEach(session -> sendEventQuietly(session, event));
    }

    private void sendEventQuietly(WebSocketSession session, ChatSocketEvent event) {
        try {
            sendEvent(session, event);
        } catch (Exception ignored) {
            sessions.remove(session);
        }
    }

    private void sendEvent(WebSocketSession session, ChatSocketEvent event) throws Exception {
        if (!session.isOpen()) {
            sessions.remove(session);
            return;
        }

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(event)));
    }
}
