package com.example.bowchat.chatroom.entity;

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

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ChatRoomParticipant> participants = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OWNER_ID")
    private User owner;

    public void registerOwner(User user) {
        ChatRoomParticipant owner = ChatRoomParticipant.create(this, user, ChatRoomParticipantRole.OWNER);
        participants.add(owner);
    }

    public void addOrActivateMember(User user) {
        ChatRoomParticipant existing = participants.stream()
                .filter(p -> p.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            if (existing.isActive()) {
                return;
            }
            existing.activate();
        } else {
            ChatRoomParticipant newParticipant = ChatRoomParticipant.create(
                    this, user, ChatRoomParticipantRole.MEMBER);
            participants.add(newParticipant);
        }
    }


}
