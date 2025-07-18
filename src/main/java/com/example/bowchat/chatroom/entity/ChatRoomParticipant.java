package com.example.bowchat.chatroom.entity;

import com.example.bowchat.user.entity.User;
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
    @JoinColumn(name = "chatroom_id")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private ChatRoomParticipantRole role; // 채팅방 참여자의 역할 (예: OWNER, MEMBER 등)
    private boolean isActive; // 현재 채팅방 참여 여부
    private Long lastReadMessageId;


    public static ChatRoomParticipant create(ChatRoom chatRoom, User user, ChatRoomParticipantRole role) {
        return ChatRoomParticipant.builder()
                .chatRoom(chatRoom)
                .user(user)
                .role(role)
                .isActive(true) // 새로 참여할 때는 활성 상태로 설정
                .lastReadMessageId(0L) // 처음 참여 시 읽은 메시지가 없으므로 0으로 초기화
                .build();
    }
    public void activate() {
        this.isActive = true; // 참여 상태를 활성화
    }
    public void deactivate() {
        this.isActive = false; // 참여 상태를 비활성화
    }
}
