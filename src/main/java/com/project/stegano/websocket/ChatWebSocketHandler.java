package com.project.stegano.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.stegano.model.ChatMessage;
import com.project.stegano.model.ChatRequest;
import com.project.stegano.model.ChatRoom;
import com.project.stegano.model.ChatSocketEvent;
import com.project.stegano.service.AuthService;
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
    private final Map<String, Set<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final ChatService chatService;

    public ChatWebSocketHandler(ObjectMapper objectMapper, ChatService chatService) {
        this.objectMapper = objectMapper;
        this.chatService = chatService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        String username = currentUsername(session);
        if (username == null) {
            sendEventQuietly(session, ChatSocketEvent.authRequired());
            return;
        }

        addSession(username, session);
        chatService.ensureGeneralRoomFor(username);
        sendEventQuietly(session, ChatSocketEvent.bootstrap(username, chatService.getRoomsForUser(username), onlineUsers()));
        sendInitialRoom(session, username);
        broadcastPresence();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String username = requireRegisteredUser(session);
            ChatRequest request = objectMapper.readValue(message.getPayload(), ChatRequest.class);
            String action = request.action() == null ? "" : request.action().trim().toLowerCase();

            switch (action) {
                case "open_room" -> openRoom(session, username, request.roomId());
                case "create_room" -> createRoom(username, request.roomName(), request.participants());
                case "open_direct" -> openDirect(session, username, request.targetUser());
                case "send" -> sendChatMessage(username, request.roomId(), request.message());
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
        String username = currentUsername(session);
        if (username != null) {
            Set<WebSocketSession> userSessions = sessionsByUser.get(username);
            if (userSessions != null) {
                userSessions.remove(session);
                if (userSessions.isEmpty()) {
                    sessionsByUser.remove(username);
                }
            }
            broadcastPresence();
        }
    }

    private void sendInitialRoom(WebSocketSession session, String username) {
        try {
            openRoom(session, username, ChatService.GENERAL_ROOM_ID);
        } catch (Exception ignored) {
            sendEventQuietly(session, ChatSocketEvent.error("Unable to load initial room"));
        }
    }

    private void openRoom(WebSocketSession session, String username, String roomId) throws Exception {
        sendEvent(session, ChatSocketEvent.roomHistory(roomId, chatService.getMessagesForRoom(username, roomId)));
    }

    private void createRoom(String username, String roomName, List<String> participants) throws Exception {
        ChatRoom room = chatService.createRoom(username, roomName, participants);
        for (String participant : room.participants()) {
            for (WebSocketSession participantSession : sessionsByUser.getOrDefault(participant, Set.of())) {
                if (participantSession.isOpen()) {
                    sendEvent(participantSession, ChatSocketEvent.roomCreated(room));
                }
            }
        }
    }

    private void openDirect(WebSocketSession session, String username, String targetUser) throws Exception {
        ChatRoom room = chatService.createOrGetDirectRoom(username, targetUser);
        for (String participant : room.participants()) {
            for (WebSocketSession participantSession : sessionsByUser.getOrDefault(participant, Set.of())) {
                if (participantSession.isOpen()) {
                    sendEvent(participantSession, ChatSocketEvent.roomCreated(room));
                }
            }
        }
        openRoom(session, username, room.id());
    }

    private void sendChatMessage(String username, String roomId, String message) throws Exception {
        ChatMessage savedMessage = chatService.sendMessage(roomId, username, message);
        ChatRoom room = chatService.getRoom(savedMessage.roomId());

        for (String participant : room.participants()) {
            for (WebSocketSession participantSession : sessionsByUser.getOrDefault(participant, Set.of())) {
                if (participantSession.isOpen()) {
                    sendEvent(participantSession, ChatSocketEvent.message(savedMessage));
                }
            }
        }
    }

    private String requireRegisteredUser(WebSocketSession session) {
        String username = currentUsername(session);
        if (username == null) {
            throw new IllegalArgumentException("Please log in first");
        }
        return username;
    }

    private String currentUsername(WebSocketSession session) {
        Object username = session.getAttributes().get(AuthService.SESSION_USERNAME);
        return username == null ? null : username.toString();
    }

    private void broadcastPresence() {
        ChatSocketEvent event = ChatSocketEvent.presence(onlineUsers());
        sessions.removeIf(ws -> !ws.isOpen());
        sessions.forEach(session -> sendEventQuietly(session, event));
    }

    private List<String> onlineUsers() {
        return sessionsByUser.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }

    private void addSession(String username, WebSocketSession session) {
        sessionsByUser.computeIfAbsent(username, ignored -> ConcurrentHashMap.newKeySet()).add(session);
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
