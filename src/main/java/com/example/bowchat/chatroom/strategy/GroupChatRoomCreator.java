package com.example.bowchat.chatroom.strategy;

import com.example.bowchat.chatroom.dto.ChatRoomCreateDTO;
import com.example.bowchat.chatroom.dto.ChatRoomResponse;
import com.example.bowchat.chatroom.entity.ChatRoom;
import com.example.bowchat.chatroom.entity.ChatRoomType;
import com.example.bowchat.chatroom.repository.ChatRoomRepository;
import com.example.bowchat.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GroupChatRoomCreator implements ChatRoomCreator<ChatRoomCreateDTO> {
    private final ChatRoomRepository chatRoomRepository;

    @Override
    public ChatRoomType roomType() {
        return ChatRoomType.GROUP;
    }

    @Override
    @Transactional
    public ChatRoomResponse createOrGet(ChatRoomCreateDTO dto, User user) {
        ChatRoom room = ChatRoom.builder()
                .name(dto.chatRoomName())
                .type(roomType())
                .owner(user)
                .build();
        room.registerOwner(user);
        return ChatRoomResponse.from(chatRoomRepository.save(room));
    }
}