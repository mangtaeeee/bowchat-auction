package com.example.chatservice.chatroom.client;

import com.example.chatservice.chatroom.client.dto.AuctionInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auction-service", url = "${auction-service.url}")
public interface AuctionServiceClient {

    @GetMapping("/internal/auctions/{auctionId}")
    AuctionInfo getAuction(@PathVariable Long auctionId);

    @GetMapping("/internal/auctions/product/{productId}")
    AuctionInfo getAuctionByProductId(@PathVariable Long productId);
}
