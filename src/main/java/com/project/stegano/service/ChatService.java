package com.project.stegano.service;

import com.project.stegano.model.ChatMessage;
import com.project.stegano.model.ChatRoom;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Service
public class ChatService {

    public static final String GENERAL_ROOM_ID = "general";

    private final AESService aesService;
    private final SteganoService steganoService;
    private final MessageStore messageStore;
    private final BufferedImage inputImage;
    private final Path outputDirectory;

    public ChatService(AESService aesService, SteganoService steganoService, MessageStore messageStore) throws Exception {
        this.aesService = aesService;
        this.steganoService = steganoService;
        this.messageStore = messageStore;
        this.inputImage = loadInputImage();
        this.outputDirectory = Path.of(System.getProperty("java.io.tmpdir"), "stegano-generated");
        Files.createDirectories(outputDirectory);
    }

    public ChatRoom ensureGeneralRoomFor(String username) {
        ChatRoom room = messageStore.getRoom(GENERAL_ROOM_ID);
        String now = Instant.now().toString();

        if (room == null) {
            room = new ChatRoom(
                    GENERAL_ROOM_ID,
                    "Community",
                    "channel",
                    List.of(username),
                    "system",
                    now
            );
            messageStore.saveRoom(room);
            return room;
        }

        if (room.participants().contains(username)) {
            return room;
        }

        ChatRoom updated = new ChatRoom(
                room.id(),
                room.name(),
                room.type(),
                mergeParticipants(room.participants(), List.of(username)),
                room.createdBy(),
                now
        );
        messageStore.saveRoom(updated);
        return updated;
    }

    public ChatRoom createRoom(String creator, String roomName, List<String> participants) {
        String cleanCreator = sanitize(creator);
        String cleanRoomName = sanitize(roomName);
        List<String> members = mergeParticipants(participants, List.of(cleanCreator));
        String roomId = "room-" + UUID.randomUUID().toString().substring(0, 8);
        String type = members.size() == 2 ? "direct" : "group";
        ChatRoom room = new ChatRoom(roomId, cleanRoomName, type, members, cleanCreator, Instant.now().toString());
        messageStore.saveRoom(room);
        return room;
    }

    public ChatMessage sendMessage(String roomId, String user, String content) throws Exception {
        ChatRoom room = requireRoom(roomId);
        String cleanUser = sanitize(user);
        if (!room.participants().contains(cleanUser)) {
            throw new IllegalArgumentException("You are not a participant in this chat");
        }

        String cleanContent = sanitize(content);
        String encrypted = aesService.encrypt(cleanContent);
        Path outputPath = outputDirectory.resolve("output_" + System.currentTimeMillis() + ".png");

        steganoService.hideMessage(inputImage, outputPath, encrypted);

        ChatMessage chatMessage = new ChatMessage(room.id(), cleanUser, cleanContent, outputPath.toString(), Instant.now().toString());
        messageStore.addMessage(chatMessage);

        ChatRoom updatedRoom = new ChatRoom(
                room.id(),
                room.name(),
                room.type(),
                room.participants(),
                room.createdBy(),
                chatMessage.sentAt()
        );
        messageStore.saveRoom(updatedRoom);

        return chatMessage;
    }

    public List<ChatRoom> getRoomsForUser(String username) {
        ensureGeneralRoomFor(username);
        return messageStore.roomsForUser(sanitize(username));
    }

    public List<ChatMessage> getMessagesForRoom(String username, String roomId) {
        ChatRoom room = requireRoom(roomId);
        if (!room.participants().contains(sanitize(username))) {
            throw new IllegalArgumentException("You are not allowed to open this chat");
        }
        return messageStore.messagesForRoom(roomId);
    }

    public ChatRoom getRoom(String roomId) {
        return requireRoom(roomId);
    }

    private ChatRoom requireRoom(String roomId) {
        String cleanRoomId = sanitize(roomId);
        ChatRoom room = messageStore.getRoom(cleanRoomId);
        if (room == null) {
            throw new IllegalArgumentException("Chat room not found");
        }
        return room;
    }

    private List<String> mergeParticipants(List<String> left, List<String> right) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (left != null) {
            left.stream().map(this::sanitize).forEach(merged::add);
        }
        if (right != null) {
            right.stream().map(this::sanitize).forEach(merged::add);
        }
        return List.copyOf(merged);
    }

    private String sanitize(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Required value is missing");
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Required value is blank");
        }

        return trimmed;
    }

    private BufferedImage loadInputImage() throws Exception {
        ClassPathResource resource = new ClassPathResource("input.png");
        try (var inputStream = resource.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IllegalStateException("Bundled input image could not be read");
            }

            return image;
        }
    }
}
