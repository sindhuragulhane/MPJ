package com.project.stegano.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.stegano.model.ChatMessage;
import com.project.stegano.model.ChatRequest;
import com.project.stegano.model.ChatRoom;
import com.project.stegano.model.ChatSocketEvent;
import com.project.stegano.service.ChatService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final Map<String, WebSocketSession> sessionsByUser = new ConcurrentHashMap<>();
    private final Map<String, String> usersBySessionId = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final ChatService chatService;

    public ChatWebSocketHandler(ObjectMapper objectMapper, ChatService chatService) {
        this.objectMapper = objectMapper;
        this.chatService = chatService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            ChatRequest request = objectMapper.readValue(message.getPayload(), ChatRequest.class);
            String action = request.action() == null ? "" : request.action().trim().toLowerCase();

            switch (action) {
                case "register" -> register(session, request.user());
                case "open_room" -> openRoom(session, request.roomId());
                case "create_room" -> createRoom(session, request.roomName(), request.participants());
                case "send" -> sendChatMessage(session, request.roomId(), request.message());
                default -> sendEvent(session, ChatSocketEvent.error("Unsupported action"));
            }
        } catch (IllegalArgumentException e) {
            sendEvent(session, ChatSocketEvent.error(e.getMessage()));
        } catch (Exception e) {
            sendEvent(session, ChatSocketEvent.error("Unable to process request"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        String username = usersBySessionId.remove(session.getId());
        if (username != null) {
            sessionsByUser.remove(username);
            broadcastPresence();
        }
    }

    private void register(WebSocketSession session, String username) throws Exception {
        String cleanUsername = requireUsername(username);
        WebSocketSession existingSession = sessionsByUser.get(cleanUsername);
        if (existingSession != null && existingSession.isOpen() && existingSession != session) {
            throw new IllegalArgumentException("That username is already active");
        }

        String previous = usersBySessionId.put(session.getId(), cleanUsername);
        if (previous != null && !previous.equals(cleanUsername)) {
            sessionsByUser.remove(previous);
        }
        sessionsByUser.put(cleanUsername, session);
        chatService.ensureGeneralRoomFor(cleanUsername);

        sendEvent(session, ChatSocketEvent.registered(cleanUsername, chatService.getRoomsForUser(cleanUsername), onlineUsers()));
        openRoom(session, ChatService.GENERAL_ROOM_ID);
        broadcastPresence();
    }

    private void openRoom(WebSocketSession session, String roomId) throws Exception {
        String username = requireRegisteredUser(session);
        sendEvent(session, ChatSocketEvent.roomHistory(roomId, chatService.getMessagesForRoom(username, roomId)));
    }

    private void createRoom(WebSocketSession session, String roomName, List<String> participants) throws Exception {
        String username = requireRegisteredUser(session);
        ChatRoom room = chatService.createRoom(username, roomName, participants);

        for (String participant : room.participants()) {
            WebSocketSession participantSession = sessionsByUser.get(participant);
            if (participantSession != null && participantSession.isOpen()) {
                sendEvent(participantSession, ChatSocketEvent.roomCreated(room));
            }
        }
    }

    private void sendChatMessage(WebSocketSession session, String roomId, String message) throws Exception {
        String username = requireRegisteredUser(session);
        ChatMessage savedMessage = chatService.sendMessage(roomId, username, message);
        ChatRoom room = chatService.getRoom(savedMessage.roomId());

        for (String participant : room.participants()) {
            WebSocketSession participantSession = sessionsByUser.get(participant);
            if (participantSession != null && participantSession.isOpen()) {
                sendEvent(participantSession, ChatSocketEvent.message(savedMessage));
            }
        }
    }

    private String requireRegisteredUser(WebSocketSession session) {
        String username = usersBySessionId.get(session.getId());
        if (username == null) {
            throw new IllegalArgumentException("Register a username first");
        }
        return username;
    }

    private String requireUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Enter a username");
        }
        return username.trim();
    }

    private void broadcastPresence() {
        ChatSocketEvent event = ChatSocketEvent.presence(onlineUsers());
        sessions.removeIf(ws -> !ws.isOpen());
        sessions.forEach(session -> sendEventQuietly(session, event));
    }

    private List<String> onlineUsers() {
        return sessionsByUser.keySet().stream().sorted().toList();
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
