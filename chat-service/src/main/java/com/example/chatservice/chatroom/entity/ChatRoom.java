package com.example.chatservice.chatroom.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "CHAT_ROOMS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CHAT_ROOM_ID")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomType type;

    private Long product;

    private Long owner;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ChatRoomParticipant> participants = new ArrayList<>();

    public void registerOwner(Long userId) {
        participants.add(ChatRoomParticipant.create(this, userId, ChatRoomParticipantRole.OWNER));
    }

    public void registerSeller(Long userId) {
        participants.add(ChatRoomParticipant.create(this, userId, ChatRoomParticipantRole.SELLER));
    }

    public void addOrActivateMember(Long userId) {
        ChatRoomParticipant existing = participants.stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            if (!existing.isActive()) existing.activate();
        } else {
            participants.add(ChatRoomParticipant.create(this, userId, ChatRoomParticipantRole.MEMBER));
        }
    }

    public void deactivateMember(Long userId) {
        participants.stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .ifPresent(ChatRoomParticipant::deactivate);
    }
}
