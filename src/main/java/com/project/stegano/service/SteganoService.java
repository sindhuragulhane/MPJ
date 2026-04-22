package com.project.stegano.service;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SteganoService {

    private final Map<Path, BufferedImage> templateCache = new ConcurrentHashMap<>();

    public void hideMessage(String inputPath, String outputPath, String message) throws Exception {
        hideMessage(Path.of(inputPath), Path.of(outputPath), message);
    }

    public void hideMessage(BufferedImage sourceImage, Path outputPath, String message) throws Exception {
        BufferedImage image = copyAsIntArgb(sourceImage);
        writeMessage(image, outputPath, message);
    }

    public void hideMessage(Path inputPath, Path outputPath, String message) throws Exception {
        BufferedImage image = copyAsIntArgb(loadTemplate(inputPath));
        writeMessage(image, outputPath, message);
    }

    private void writeMessage(BufferedImage image, Path outputPath, String message) throws Exception {
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        byte[] msgBytes = message.getBytes(StandardCharsets.UTF_8);
        int requiredBits = 32 + (msgBytes.length * 8);

        if (requiredBits > pixels.length) {
            throw new IllegalArgumentException("Message is too large for the selected image");
        }

        // store length
        for (int i = 0; i < 32; i++) {
            int bit = (msgBytes.length >> (31 - i)) & 1;
            pixels[i] = (pixels[i] & 0xFFFFFFFE) | bit;
        }

        int bitIndex = 32;

        for (byte b : msgBytes) {
            for (int i = 7; i >= 0; i--) {
                int bit = (b >> i) & 1;
                pixels[bitIndex] = (pixels[bitIndex] & 0xFFFFFFFE) | bit;
                bitIndex++;
            }
        }

        ImageIO.write(image, "png", outputPath.toFile());
    }

    public String extractMessage(String path) throws Exception {
        return extractMessage(Path.of(path));
    }

    public String extractMessage(Path path) throws Exception {
        BufferedImage image = copyAsIntArgb(ImageIO.read(path.toFile()));
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        int len = 0;

        for (int i = 0; i < 32; i++) {
            int bit = pixels[i] & 1;
            len = (len << 1) | bit;
        }

        byte[] msg = new byte[len];
        int bitIndex = 32;

        for (int i = 0; i < len; i++) {
            int b = 0;

            for (int j = 0; j < 8; j++) {
                int bit = pixels[bitIndex] & 1;
                b = (b << 1) | bit;
                bitIndex++;
            }

            msg[i] = (byte) b;
        }

        return new String(msg, StandardCharsets.UTF_8);
    }

    private BufferedImage loadTemplate(Path inputPath) {
        Path normalizedPath = inputPath.toAbsolutePath().normalize();
        return templateCache.computeIfAbsent(normalizedPath, path -> {
            try {
                return copyAsIntArgb(ImageIO.read(path.toFile()));
            } catch (Exception e) {
                throw new IllegalStateException("Unable to load the source image", e);
            }
        });
    }

    private BufferedImage copyAsIntArgb(BufferedImage source) {
        if (source == null) {
            throw new IllegalArgumentException("Unable to read the source image");
        }

        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }

        return copy;
    }
}
