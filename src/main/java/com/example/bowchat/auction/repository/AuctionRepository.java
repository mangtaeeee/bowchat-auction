package com.example.bowchat.auction.repository;

import com.example.bowchat.auction.entity.Auction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {

    @Query("SELECT a FROM Auction a JOIN FETCH a.product p JOIN FETCH p.seller WHERE a.id = :id")
    Optional<Auction> findWithProductAndSellerById(Long id);

    @Query("SELECT a FROM Auction a JOIN FETCH a.product p JOIN FETCH p.seller WHERE p.id = :productId")
    Optional<Auction> findByProductId(Long productId);


}
