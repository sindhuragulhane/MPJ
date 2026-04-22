package com.project.stegano.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Service
public class AESService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AESService(@Value("${app.crypto.key}") String configuredKey) {
        this.secretKey = new SecretKeySpec(deriveKey(configuredKey), "AES");
    }

    public String encrypt(String data) throws Exception {
        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

        byte[] compressed = compress(data.getBytes(StandardCharsets.UTF_8));
        byte[] encrypted = cipher.doFinal(compressed);

        byte[] payload = new byte[IV_LENGTH + encrypted.length];
        System.arraycopy(iv, 0, payload, 0, IV_LENGTH);
        System.arraycopy(encrypted, 0, payload, IV_LENGTH, encrypted.length);

        return Base64.getEncoder().encodeToString(payload);
    }

    public String decrypt(String encryptedData) throws Exception {
        byte[] payload = Base64.getDecoder().decode(encryptedData);
        if (payload.length <= IV_LENGTH) {
            throw new IllegalArgumentException("Encrypted payload is invalid");
        }

        byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH);
        byte[] encrypted = Arrays.copyOfRange(payload, IV_LENGTH, payload.length);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        byte[] decompressed = decompress(cipher.doFinal(encrypted));

        return new String(decompressed, StandardCharsets.UTF_8);
    }

    private byte[] deriveKey(String value) {
        try {
            return Arrays.copyOf(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)),
                    16
            );
        } catch (Exception e) {
            throw new IllegalStateException("Unable to derive encryption key", e);
        }
    }

    private byte[] compress(byte[] input) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream)) {
            gzipOutputStream.write(input);
        }
        return outputStream.toByteArray();
    }

    private byte[] decompress(byte[] input) throws Exception {
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(input))) {
            return gzipInputStream.readAllBytes();
        }
    }
}
