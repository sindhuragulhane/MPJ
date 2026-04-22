package com.project.stegano.service;

import com.project.stegano.model.ChatMessage;
import com.project.stegano.model.ChatRoom;
import com.project.stegano.persistence.ChatMessageEntity;
import com.project.stegano.persistence.ChatMessageRepository;
import com.project.stegano.persistence.ChatRoomEntity;
import com.project.stegano.persistence.ChatRoomRepository;
import com.project.stegano.persistence.RoomMembershipEntity;
import com.project.stegano.persistence.RoomMembershipRepository;
import com.project.stegano.persistence.UserAccountEntity;
import com.project.stegano.persistence.UserAccountRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MessageStore {

    private final UserAccountRepository userAccountRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final RoomMembershipRepository roomMembershipRepository;
    private final ChatMessageRepository chatMessageRepository;

    public MessageStore(
            UserAccountRepository userAccountRepository,
            ChatRoomRepository chatRoomRepository,
            RoomMembershipRepository roomMembershipRepository,
            ChatMessageRepository chatMessageRepository
    ) {
        this.userAccountRepository = userAccountRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.roomMembershipRepository = roomMembershipRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    public UserAccountEntity requireUser(String username) {
        return userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public ChatRoomEntity saveRoom(ChatRoomEntity room) {
        return chatRoomRepository.save(room);
    }

    public ChatRoomEntity requireRoom(String roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found"));
    }

    public boolean roomExists(String roomId) {
        return chatRoomRepository.existsById(roomId);
    }

    public void addMembership(ChatRoomEntity room, UserAccountEntity user, String joinedAt) {
        if (roomMembershipRepository.existsByRoomIdAndUserUsername(room.getId(), user.getUsername())) {
            return;
        }

        RoomMembershipEntity membership = new RoomMembershipEntity();
        membership.setRoom(room);
        membership.setUser(user);
        membership.setJoinedAt(joinedAt);
        roomMembershipRepository.save(membership);
    }

    public boolean isMember(String roomId, String username) {
        return roomMembershipRepository.existsByRoomIdAndUserUsername(roomId, username);
    }

    public List<ChatRoom> roomsForUser(String username) {
        return roomMembershipRepository.findByUserUsernameOrderByRoomUpdatedAtDesc(username).stream()
                .map(membership -> toRoomDto(membership.getRoom()))
                .toList();
    }

    public List<String> participantsForRoom(String roomId) {
        return roomMembershipRepository.findByRoomId(roomId).stream()
                .map(membership -> membership.getUser().getUsername())
                .toList();
    }

    public ChatMessageEntity saveMessage(ChatMessageEntity messageEntity) {
        return chatMessageRepository.save(messageEntity);
    }

    public List<ChatMessageEntity> messagesForRoom(String roomId) {
        return chatMessageRepository.findByRoomIdOrderBySentAtAsc(roomId);
    }

    public ChatRoom toRoomDto(ChatRoomEntity roomEntity) {
        return new ChatRoom(
                roomEntity.getId(),
                roomEntity.getName(),
                roomEntity.getType(),
                participantsForRoom(roomEntity.getId()),
                roomEntity.getCreatedBy(),
                roomEntity.getUpdatedAt()
        );
    }

    public ChatMessage toMessageDto(ChatMessageEntity entity, String content) {
        return new ChatMessage(
                entity.getRoom().getId(),
                entity.getSender().getUsername(),
                content,
                entity.getImagePath(),
                entity.getSentAt()
        );
    }
}
