package com.example.bowchat.auction.repository;

import com.example.bowchat.auction.entity.AuctionBid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuctionBidRepository extends JpaRepository<AuctionBid, Long> {
    List<AuctionBid> findByAuction_IdOrderByBidTimeAsc(Long auctionId);
}
