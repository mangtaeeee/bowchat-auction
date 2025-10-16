package com.example.bowchat.kafka;

import com.example.bowchat.chatmessage.entity.MessageType;
import lombok.Builder;

import java.time.Instant;

// 카프카를 통해 전송되는 채팅 공통 메시지 구조 정의
@Builder
public record ChatEvent(
        Long roomId,
        Long senderId,
        String senderName,
        MessageType type,
        String content,   // 실제 메시지 내용
        Long timestamp
) {
    public String partitionKey() {
        // 경매 이벤트는 "AUCTION:<roomId>" 같은 prefix를 추가해 채팅과 키 공간을 분리
        if (type == MessageType.AUCTION_BID
                || type == MessageType.AUCTION_END
                || type == MessageType.AUCTION_BROADCAST) {
            return "AUCTION:" + roomId; // roomId == auctionId로 매핑됨
        }
        // 일반 채팅은 채팅방 단위로 직렬화
        return "CHATROOM:" + roomId;
    }

    public static ChatEvent enrichChatEvent(Long roomId, ChatEvent original) {
        return ChatEvent.builder()
                .roomId(roomId)
                .senderId(original.senderId())
                .senderName(original.senderName())
                .type(original.type)
                .content(original.content())
                .timestamp(Instant.now().toEpochMilli())
                .build();
    }
}
