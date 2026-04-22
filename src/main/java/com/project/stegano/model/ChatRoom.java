package com.project.stegano.model;

import java.util.List;

public record ChatRoom(
        String id,
        String name,
        String type,
        List<String> participants,
        String createdBy,
        String updatedAt
) {
}
