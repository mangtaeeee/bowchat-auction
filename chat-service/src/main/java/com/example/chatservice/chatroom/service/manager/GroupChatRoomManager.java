package com.example.chatservice.chatroom.service.manager;


import com.example.chatservice.chatroom.dto.request.GroupChatRoomEnterRequest;
import com.example.chatservice.chatroom.dto.response.EnterChatResponse;
import com.example.chatservice.chatroom.entity.ChatRoom;
import com.example.chatservice.chatroom.entity.ChatRoomType;
import com.example.chatservice.chatroom.repository.ChatRoomRepository;
import com.example.chatservice.chatroom.service.ChatRoomManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GroupChatRoomManager implements ChatRoomManager<GroupChatRoomEnterRequest> {

    private final ChatRoomRepository chatRoomRepository;

    @Override
    public ChatRoomType supportType() {
        return ChatRoomType.GROUP;
    }

    @Override
    public Class<GroupChatRoomEnterRequest> requestType() {
        return GroupChatRoomEnterRequest.class;
    }

    @Override
    @Transactional
    public EnterChatResponse enterChatRoom(GroupChatRoomEnterRequest request) {
        ChatRoom room = ChatRoom.builder()
                .name(request.getRoomName())
                .type(ChatRoomType.GROUP)
                .owner(request.getUserId())
                .build();
        room.registerOwner(request.getUserId());
        ChatRoom saved = chatRoomRepository.save(room);

        return new EnterChatResponse(saved.getId(), ChatRoomType.GROUP, saved.getName());
    }
}
