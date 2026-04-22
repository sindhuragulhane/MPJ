package com.project.stegano;

import com.project.stegano.model.ChatMessage;
import com.project.stegano.model.ChatRoom;
import com.project.stegano.service.ChatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SteganoController {

    private final ChatService chatService;

    public SteganoController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/rooms")
    public List<ChatRoom> getRooms(@RequestParam String user) {
        return chatService.getRoomsForUser(user);
    }

    @GetMapping("/messages")
    public List<ChatMessage> getMessages(@RequestParam String user, @RequestParam String roomId) {
        return chatService.getMessagesForRoom(user, roomId);
    }
}
