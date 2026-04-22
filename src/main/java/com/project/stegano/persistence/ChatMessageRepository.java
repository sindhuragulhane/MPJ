package com.project.stegano.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {
    List<ChatMessageEntity> findByRoomIdOrderBySentAtAsc(String roomId);
}
