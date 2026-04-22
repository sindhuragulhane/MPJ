package com.project.stegano.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomMembershipRepository extends JpaRepository<RoomMembershipEntity, Long> {
    List<RoomMembershipEntity> findByUserUsernameOrderByRoomUpdatedAtDesc(String username);
    List<RoomMembershipEntity> findByRoomId(String roomId);
    boolean existsByRoomIdAndUserUsername(String roomId, String username);
    Optional<RoomMembershipEntity> findByRoomIdAndUserUsername(String roomId, String username);
}
