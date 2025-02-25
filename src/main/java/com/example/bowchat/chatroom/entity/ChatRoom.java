package com.example.bowchat.chatroom.entity;

import jakarta.persistence.*;
import lombok.*;

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

}
