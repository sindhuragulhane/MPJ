package com.project.stegano;

import com.project.stegano.model.ChatMessage;
import com.project.stegano.model.ChatRoom;
import com.project.stegano.service.AuthService;
import com.project.stegano.service.ChatService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SteganoController {

    private final ChatService chatService;
    private final AuthService authService;

    public SteganoController(ChatService chatService, AuthService authService) {
        this.chatService = chatService;
        this.authService = authService;
    }

    @GetMapping("/rooms")
    public List<ChatRoom> getRooms(HttpSession session) {
        return chatService.getRoomsForUser(authService.currentUsername(session));
    }

    @GetMapping("/messages")
    public List<ChatMessage> getMessages(@RequestParam String roomId, HttpSession session) {
        return chatService.getMessagesForRoom(authService.currentUsername(session), roomId);
    }
}
