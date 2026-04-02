package com.example.bowchat.kafkastarter.event;


import lombok.Builder;

import java.time.Instant;
import java.util.Objects;

// 카프카를 통해 전송되는 채팅 공통 메시지 구조 정의
@Builder
public record EventMessage(
        Long roomId,
        Long senderId,
        String senderName,
        String topicName,
        String messageType,
        String content,   // 실제 메시지 내용
        Long timestamp
) {
    public String partitionKey() {
        // 경매 이벤트는 "AUCTION:<roomId>" 같은 prefix를 추가해 채팅과 키 공간을 분리
        if (Objects.equals(messageType, MessageType.AUCTION_BID.name())
                || Objects.equals(messageType, MessageType.AUCTION_END.name())
                || Objects.equals(messageType, MessageType.AUCTION_BROADCAST.name())) {
            return "AUCTION:" + roomId; // roomId == auctionId로 매핑됨
        }
        // 일반 채팅은 채팅방 단위로 직렬화
        return "CHATROOM:" + roomId;
    }

    public static EventMessage enrichChatEvent(Long roomId, EventMessage original) {
        return EventMessage.builder()
                .roomId(roomId)
                .senderId(original.senderId())
                .senderName(original.senderName())
                .topicName(original.topicName)
                .messageType(original.messageType)
                .content(original.content())
                .timestamp(Instant.now().toEpochMilli())
                .build();
    }
}
