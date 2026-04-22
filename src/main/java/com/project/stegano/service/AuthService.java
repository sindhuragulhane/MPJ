package com.project.stegano.service;

import com.project.stegano.persistence.UserAccountEntity;
import com.project.stegano.persistence.UserAccountRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthService {

    public static final String SESSION_USERNAME = "AUTHENTICATED_USERNAME";

    private final UserAccountRepository userAccountRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public String register(String username, String password) {
        String cleanUsername = sanitizeUsername(username);
        String cleanPassword = sanitizePassword(password);

        if (userAccountRepository.existsByUsername(cleanUsername)) {
            throw new IllegalArgumentException("Username already exists");
        }

        UserAccountEntity entity = new UserAccountEntity();
        entity.setUsername(cleanUsername);
        entity.setPasswordHash(passwordEncoder.encode(cleanPassword));
        entity.setCreatedAt(Instant.now().toString());
        userAccountRepository.save(entity);

        return cleanUsername;
    }

    public String login(String username, String password, HttpSession session) {
        String cleanUsername = sanitizeUsername(username);
        String cleanPassword = sanitizePassword(password);

        UserAccountEntity entity = userAccountRepository.findByUsername(cleanUsername)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(cleanPassword, entity.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        session.setAttribute(SESSION_USERNAME, cleanUsername);
        return cleanUsername;
    }

    public String currentUsername(HttpSession session) {
        Object value = session.getAttribute(SESSION_USERNAME);
        if (value == null) {
            throw new IllegalArgumentException("You need to log in first");
        }
        return value.toString();
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }

    public boolean userExists(String username) {
        return userAccountRepository.existsByUsername(username);
    }

    private String sanitizeUsername(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username is required");
        }
        String trimmed = username.trim();
        if (trimmed.length() < 3 || trimmed.length() > 30 || !trimmed.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Username must be 3-30 chars and use letters, numbers, or _");
        }
        return trimmed;
    }

    private String sanitizePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        return password;
    }
}
