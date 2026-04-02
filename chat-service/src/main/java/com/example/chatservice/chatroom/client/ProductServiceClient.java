package com.example.chatservice.chatroom.client;

import com.example.chatservice.chatroom.client.dto.ProductInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", url = "${product-service.url}")
public interface ProductServiceClient {

    @GetMapping("/internal/products/{productId}/seller")
    Long getSellerId(@PathVariable Long productId);

    @GetMapping("/internal/products/{productId}")
    ProductInfo getProduct(@PathVariable Long productId);
}
