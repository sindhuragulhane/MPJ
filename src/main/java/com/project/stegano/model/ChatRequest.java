package com.project.stegano.model;

import java.util.List;

public record ChatRequest(
        String action,
        String roomId,
        String roomName,
        String targetUser,
        List<String> participants,
        String message
) {
}
