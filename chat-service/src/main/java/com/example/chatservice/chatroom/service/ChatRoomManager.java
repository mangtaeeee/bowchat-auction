package com.example.chatservice.chatroom.service;

import com.example.chatservice.chatroom.dto.request.ChatRoomEnterRequest;
import com.example.chatservice.chatroom.dto.response.EnterChatResponse;
import com.example.chatservice.chatroom.entity.ChatRoomType;

public interface ChatRoomManager<T extends ChatRoomEnterRequest> {

    ChatRoomType supportType();

    Class<T> requestType();

    EnterChatResponse enterChatRoom(T request, Long userId);

    // 타입 캐스팅을 안전하게 처리하는 default 메서드
    // 새 Manager 추가 시 이 메서드를 오버라이드할 필요 없음
    default EnterChatResponse enter(ChatRoomEnterRequest request, Long userId) {
        return enterChatRoom(requestType().cast(request), userId);
    }
}
