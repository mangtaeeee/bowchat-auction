package com.example.bowchat.kafka;

import com.example.bowchat.auction.service.AuctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuctionBidConsumer {

    private final AuctionService auctionService;

    // Kafka에서 경매 입찰 메시지를 수신하고 처리
    @KafkaListener(topics = "auction-bid", groupId = "auction-bid-group")
    public void handleBid(ChatEvent event) {
        Long auctionId = event.roomId();
        Long userId    = event.senderId();
        Long amount    = Long.valueOf(event.content());
        auctionService.placeBid(auctionId, userId, amount);
    }
}
