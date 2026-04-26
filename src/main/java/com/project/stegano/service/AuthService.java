package com.project.stegano.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    public static final String SESSION_USERNAME = "AUTHENTICATED_USERNAME";

    private final Map<String, String> users = new ConcurrentHashMap<>();

    public String register(String username, String password, HttpSession session) {
        String cleanUsername = sanitizeUsername(username);
        String cleanPassword = sanitizePassword(password);

        if (users.putIfAbsent(cleanUsername, hash(cleanPassword)) != null) {
            throw new IllegalArgumentException("Username already exists");
        }

        session.setAttribute(SESSION_USERNAME, cleanUsername);
        return cleanUsername;
    }

    public String login(String username, String password, HttpSession session) {
        String cleanUsername = sanitizeUsername(username);
        String cleanPassword = sanitizePassword(password);
        String expected = users.get(cleanUsername);

        if (expected == null || !expected.equals(hash(cleanPassword))) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        session.setAttribute(SESSION_USERNAME, cleanUsername);
        return cleanUsername;
    }

    public String currentUsername(HttpSession session) {
        Object value = session.getAttribute(SESSION_USERNAME);
        if (value == null) {
            throw new IllegalArgumentException("Please log in first");
        }
        return value.toString();
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }

    public boolean userExists(String username) {
        return users.containsKey(username);
    }

    private String sanitizeUsername(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username is required");
        }

        String trimmed = username.trim();
        if (!trimmed.matches("[A-Za-z0-9_]{3,24}")) {
            throw new IllegalArgumentException("Username must be 3-24 characters using letters, numbers, or _");
        }

        return trimmed;
    }

    private String sanitizePassword(String password) {
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        return password;
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : digest) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash password", e);
        }
    }
}
