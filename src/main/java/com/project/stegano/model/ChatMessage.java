package com.project.stegano.model;

public record ChatMessage(
        String user,
        String content,
        String imagePath,
        String sentAt
) {
}
