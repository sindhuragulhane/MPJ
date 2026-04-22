package com.project.stegano;

import com.project.stegano.model.ChatMessage;
import com.project.stegano.model.SendMessageRequest;
import com.project.stegano.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class SteganoController {

    private final ChatService chatService;

    public SteganoController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestBody SendMessageRequest request) {
        try {
            ChatMessage message = chatService.sendMessage(request.user(), request.message());
            return ResponseEntity.ok(message);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Unable to send message"));
        }
    }

    @GetMapping("/send")
    public ResponseEntity<?> sendCompatibility(@RequestParam String message, @RequestParam String user) {
        try {
            ChatMessage chatMessage = chatService.sendMessage(user, message);
            return ResponseEntity.ok(chatMessage);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Unable to send message"));
        }
    }

    @GetMapping("/messages")
    public List<ChatMessage> getMessages() {
        return chatService.getMessages();
    }
}
