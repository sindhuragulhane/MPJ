package com.project.stegano.model;

import java.util.List;

public record ChatRequest(
        String action,
        String user,
        String roomId,
        String roomName,
        List<String> participants,
        String message
) {
}
