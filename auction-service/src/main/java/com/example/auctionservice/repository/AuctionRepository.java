package com.example.auctionservice.repository;

import com.example.auctionservice.entity.Auction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {

    Optional<Auction> findByProduct(Long productId);

    // 진행 중인 경매 조회
    @Query("SELECT a FROM Auction a WHERE a.endTime > :now")
    List<Auction> findActiveAuctions(@Param("now") LocalDateTime now);
}
