package com.example.chatservice.chatroom.dto.request;

import com.example.chatservice.chatroom.entity.ChatRoomType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "그룹 채팅방 입장 요청")
public class GroupChatRoomEnterRequest extends ChatRoomEnterRequest {

    @Schema(description = "생성할 그룹 채팅방 이름", example = "backend-study")
    private String roomName;

    @Override
    public ChatRoomType getRoomType() {
        return ChatRoomType.GROUP;
    }
}
