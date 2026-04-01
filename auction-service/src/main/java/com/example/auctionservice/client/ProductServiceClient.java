package com.example.auctionservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// ProductServiceClient
@FeignClient(name = "product-service", url = "${product-service.url}")
public interface ProductServiceClient {

    @GetMapping("/internal/products/{productId}/seller")
    Long getSellerId(@PathVariable Long productId);
}