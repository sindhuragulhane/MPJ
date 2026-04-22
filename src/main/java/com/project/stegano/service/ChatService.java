package com.project.stegano.service;

import com.project.stegano.model.ChatMessage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

@Service
public class ChatService {

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

    public ChatMessage sendMessage(String user, String content) throws Exception {
        String cleanUser = sanitize(user);
        String cleanContent = sanitize(content);
        String encrypted = aesService.encrypt(cleanContent);
        Path outputPath = outputDirectory.resolve("output_" + System.currentTimeMillis() + ".png");

        steganoService.hideMessage(inputImage, outputPath, encrypted);

        ChatMessage chatMessage = new ChatMessage(cleanUser, cleanContent, outputPath.toString(), Instant.now().toString());
        messageStore.add(chatMessage);
        return chatMessage;
    }

    public List<ChatMessage> getMessages() {
        return messageStore.snapshot();
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
