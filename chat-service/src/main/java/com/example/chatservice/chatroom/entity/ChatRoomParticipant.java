package com.example.chatservice.chatroom.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "CHATROOM_PARTICIPANTS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatRoomParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatroom_id", nullable = false)
    private ChatRoom chatRoom;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomParticipantRole role;

    private boolean isActive;

    private Long lastReadMessageId;

    public static ChatRoomParticipant create(ChatRoom chatRoom, Long userId, ChatRoomParticipantRole role) {
        return ChatRoomParticipant.builder()
                .chatRoom(chatRoom)
                .userId(userId)
                .role(role)
                .isActive(true)
                .lastReadMessageId(0L)
                .build();
    }

    public void activate() { this.isActive = true; }
    public void deactivate() { this.isActive = false; }
}