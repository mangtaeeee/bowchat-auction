package com.example.auctionservice.repository;

import com.example.auctionservice.entity.AuctionBid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuctionBidRepository extends JpaRepository<AuctionBid, Long> {
    List<AuctionBid> findByAuction_IdOrderByBidTimeAsc(Long auctionId);
}
