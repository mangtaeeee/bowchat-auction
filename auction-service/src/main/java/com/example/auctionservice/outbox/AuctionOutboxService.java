package com.example.auctionservice.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuctionOutboxService {

    public static final String AUCTION_AGGREGATE = "AUCTION";
    public static final String AUCTION_BID_PLACED = "AUCTION_BID_PLACED";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    // 입찰 트랜잭션 안에서 통합 이벤트를 함께 저장한다.
    // AFTER_COMMIT 리스너는 커밋 이후에 실행되므로, 발행 전에 장애가 나면 이벤트가 유실될 수 있다.
    @Transactional(propagation = Propagation.MANDATORY)
    public void appendBidPlacedEvent(Long auctionId, Long bidderId, String bidderNickname, Long bidAmount, long occurredAt) {
        AuctionBidPlacedPayload payload = AuctionBidPlacedPayload.of(
                auctionId,
                bidderId,
                bidderNickname,
                bidAmount,
                occurredAt
        );

        outboxEventRepository.save(
                OutboxEvent.pending(
                        AUCTION_AGGREGATE,
                        auctionId,
                        AUCTION_BID_PLACED,
                        toJson(payload),
                        LocalDateTime.now()
                )
        );
    }

    private String toJson(AuctionBidPlacedPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("경매 이벤트를 Outbox에 저장할 형식으로 바꾸는 중 문제가 생겼습니다.", e);
        }
    }
}
