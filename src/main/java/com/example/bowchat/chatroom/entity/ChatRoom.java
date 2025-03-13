package com.example.bowchat.chatroom.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @ElementCollection
    @CollectionTable(name = "CHATROOM_PARTICIPANTS", joinColumns = @JoinColumn(name = "CHATROOM_ID"))
    @Column(name = "PARTICIPANT")
    private List<String> participants;

}
