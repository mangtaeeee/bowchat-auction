package com.example.bowchat.kafka;

import com.example.bowchat.chatmessage.entity.MessageType;
import java.time.Instant;

public class ChatEventFactory {

    public static ChatEvent auctionBid(Long roomId, Long senderId, String senderName, Long amount) {
        return ChatEvent.builder()
                .roomId(roomId)
                .senderId(senderId)
                .senderName(senderName)
                .type(MessageType.AUCTION_BROADCAST)
                .content(String.valueOf(amount))
                .timestamp(Instant.now().toEpochMilli())
                .build();
    }

}