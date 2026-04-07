package com.example.auctionservice.repository;

import com.example.auctionservice.entity.Auction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AuctionRepositoryLockTest {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void saveAndFlushThrowsOptimisticLockExceptionForStaleAuctionVersion() {
        // 서로 다른 트랜잭션에서 같은 경매를 읽으면, 먼저 반영된 입찰 이후의 stale 엔티티는 저장에 실패해야 한다.
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        Long auctionId = tx.execute(status -> auctionRepository.saveAndFlush(createAuction()).getId());

        Auction firstBidderView = tx.execute(status -> auctionRepository.findById(auctionId).orElseThrow());
        Auction secondBidderView = tx.execute(status -> auctionRepository.findById(auctionId).orElseThrow());

        firstBidderView.placeBid(2L, 15_000L, LocalDateTime.now());
        tx.executeWithoutResult(status -> auctionRepository.saveAndFlush(firstBidderView));

        secondBidderView.placeBid(3L, 20_000L, LocalDateTime.now());

        assertThatThrownBy(() ->
                tx.executeWithoutResult(status -> auctionRepository.saveAndFlush(secondBidderView)))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    private Auction createAuction() {
        return Auction.builder()
                .product(10L)
                .sellerId(1L)
                .startTime(LocalDateTime.now().minusMinutes(1))
                .endTime(LocalDateTime.now().plusMinutes(30))
                .startingPrice(10_000L)
                .currentPrice(10_000L)
                .build();
    }
}
