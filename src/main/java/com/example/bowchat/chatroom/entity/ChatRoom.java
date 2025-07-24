package com.example.bowchat.chatroom.entity;

import com.example.bowchat.product.entity.Product;
import com.example.bowchat.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "CHAT_ROOMS")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CHAT_ROOM_ID")
    private Long id;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCT_ID")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OWNER_ID")
    private User owner;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ChatRoomParticipant> participants = new ArrayList<>();


    public void registerOwner(User user) {
        ChatRoomParticipant owner = ChatRoomParticipant.create(this, user, ChatRoomParticipantRole.OWNER);
        participants.add(owner);
    }

    public void deactivateMember(User user) {
        participants.stream()
                .filter(p -> p.getUser().getId().equals(user.getId()))
                .findFirst()
                .ifPresent(ChatRoomParticipant::deactivate);
    }

    public void addOrActivateMember(User user) {
        ChatRoomParticipant existing = participants.stream()
                .filter(p -> p.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElse(null);

        //입장내역은 있지만 비활성화 회원의 경우
        if (existing != null) {
            if (existing.isActive()) {
                return;
            }
            existing.activate();
        } else {
            // 입장 내역이 없는 회원의 경우
            ChatRoomParticipant newParticipant = ChatRoomParticipant.create(
                    this, user, ChatRoomParticipantRole.MEMBER);
            participants.add(newParticipant);
        }
    }
}
