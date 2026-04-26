package com.project.stegano.model;

public record AuthResponse(
        boolean authenticated,
        String username,
        String error
) {
    public static AuthResponse success(String username) {
        return new AuthResponse(true, username, null);
    }

    public static AuthResponse failure(String error) {
        return new AuthResponse(false, null, error);
    }
}
