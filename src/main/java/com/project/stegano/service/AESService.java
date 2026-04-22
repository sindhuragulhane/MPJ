package com.project.stegano.service;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class AESService {

    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final SecretKeySpec SECRET_KEY =
            new SecretKeySpec("1234567890123456".getBytes(StandardCharsets.UTF_8), "AES");
    private static final ThreadLocal<Cipher> ENCRYPT_CIPHER = ThreadLocal.withInitial(() -> newCipher());
    private static final ThreadLocal<Cipher> DECRYPT_CIPHER = ThreadLocal.withInitial(() -> newCipher());

    public String encrypt(String data) throws Exception {
        Cipher cipher = ENCRYPT_CIPHER.get();
        cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY);
        byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(encrypted);
    }

    public String decrypt(String encryptedData) throws Exception {
        Cipher cipher = DECRYPT_CIPHER.get();
        cipher.init(Cipher.DECRYPT_MODE, SECRET_KEY);
        byte[] decoded = Base64.getDecoder().decode(encryptedData);

        return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
    }

    private static Cipher newCipher() {
        try {
            return Cipher.getInstance(TRANSFORMATION);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create AES cipher", e);
        }
    }
}
