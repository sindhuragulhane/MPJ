package com.project.stegano.service;

import com.project.stegano.model.ChatMessage;
import com.project.stegano.model.ChatRoom;
import com.project.stegano.persistence.ChatMessageEntity;
import com.project.stegano.persistence.ChatRoomEntity;
import com.project.stegano.persistence.UserAccountEntity;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final AuthService authService;
    private final RateLimiterService rateLimiterService;
    private final BufferedImage inputImage;
    private final Path outputDirectory;

    public ChatService(
            AESService aesService,
            SteganoService steganoService,
            MessageStore messageStore,
            AuthService authService,
            RateLimiterService rateLimiterService
    ) throws Exception {
        this.aesService = aesService;
        this.steganoService = steganoService;
        this.messageStore = messageStore;
        this.authService = authService;
        this.rateLimiterService = rateLimiterService;
        this.inputImage = loadInputImage();
        this.outputDirectory = Path.of(System.getProperty("java.io.tmpdir"), "stegano-generated");
        Files.createDirectories(outputDirectory);
    }

    @Transactional
    public ChatRoom ensureGeneralRoomFor(String username) {
        messageStore.requireUser(username);
        String now = Instant.now().toString();
        ChatRoomEntity room;

        if (messageStore.roomExists(GENERAL_ROOM_ID)) {
            room = messageStore.requireRoom(GENERAL_ROOM_ID);
        } else {
            room = new ChatRoomEntity();
            room.setId(GENERAL_ROOM_ID);
            room.setName("Community");
            room.setType("channel");
            room.setCreatedBy("system");
            room.setUpdatedAt(now);
            room = messageStore.saveRoom(room);
        }

        messageStore.addMembership(room, messageStore.requireUser(username), now);
        return messageStore.toRoomDto(room);
    }

    @Transactional
    public ChatRoom createRoom(String creator, String roomName, List<String> participants) {
        rateLimiterService.checkLimit("room-create", creator, 8, 300);

        UserAccountEntity creatorEntity = messageStore.requireUser(creator);
        String cleanRoomName = sanitizeLabel(roomName, "Room name");
        List<String> memberUsernames = mergeParticipants(participants, List.of(creator));
        if (memberUsernames.size() < 2) {
            throw new IllegalArgumentException("Choose at least one other valid participant");
        }

        String roomId = "room-" + UUID.randomUUID().toString().substring(0, 8);
        String now = Instant.now().toString();

        ChatRoomEntity room = new ChatRoomEntity();
        room.setId(roomId);
        room.setName(cleanRoomName);
        room.setType(memberUsernames.size() == 2 ? "direct" : "group");
        room.setCreatedBy(creator);
        room.setUpdatedAt(now);
        room = messageStore.saveRoom(room);

        for (String username : memberUsernames) {
            messageStore.addMembership(room, messageStore.requireUser(username), now);
        }

        return messageStore.toRoomDto(room);
    }

    @Transactional
    public ChatMessage sendMessage(String roomId, String username, String content) throws Exception {
        rateLimiterService.checkLimit("message-send", username, 25, 60);

        ChatRoomEntity room = messageStore.requireRoom(requireValue(roomId, "Room"));
        if (!messageStore.isMember(room.getId(), username)) {
            throw new IllegalArgumentException("You are not allowed to post in this room");
        }

        String cleanContent = sanitizeLabel(content, "Message");
        String encrypted = aesService.encrypt(cleanContent);
        Path outputPath = outputDirectory.resolve("output_" + System.currentTimeMillis() + ".png");
        steganoService.hideMessage(inputImage, outputPath, encrypted);

        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setRoom(room);
        entity.setSender(messageStore.requireUser(username));
        entity.setEncryptedContent(encrypted);
        entity.setImagePath(outputPath.toString());
        entity.setSentAt(Instant.now().toString());
        entity = messageStore.saveMessage(entity);

        room.setUpdatedAt(entity.getSentAt());
        messageStore.saveRoom(room);

        return messageStore.toMessageDto(entity, cleanContent);
    }

    public List<ChatRoom> getRoomsForUser(String username) {
        ensureGeneralRoomFor(username);
        return messageStore.roomsForUser(username);
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getMessagesForRoom(String username, String roomId) {
        if (!messageStore.isMember(roomId, username)) {
            throw new IllegalArgumentException("You are not allowed to open this chat");
        }

        return messageStore.messagesForRoom(roomId).stream().map(entity -> {
            try {
                return messageStore.toMessageDto(entity, aesService.decrypt(entity.getEncryptedContent()));
            } catch (Exception e) {
                throw new IllegalStateException("Unable to decrypt stored message", e);
            }
        }).toList();
    }

    @Transactional(readOnly = true)
    public ChatRoom getRoom(String roomId) {
        return messageStore.toRoomDto(messageStore.requireRoom(roomId));
    }

    private List<String> mergeParticipants(List<String> left, List<String> right) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (left != null) {
            left.stream().map(username -> requireValue(username, "Participant")).forEach(merged::add);
        }
        if (right != null) {
            right.stream().map(username -> requireValue(username, "Participant")).forEach(merged::add);
        }
        merged.forEach(messageStore::requireUser);
        return List.copyOf(merged);
    }

    private String sanitizeLabel(String value, String label) {
        String trimmed = requireValue(value, label);
        if (trimmed.length() > 150) {
            throw new IllegalArgumentException(label + " is too long");
        }
        return trimmed;
    }

    private String requireValue(String value, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " is required");
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be blank");
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
