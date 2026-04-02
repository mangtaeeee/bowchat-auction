package com.example.auctionservice.dto.request;


import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record StartAuctionRequest(
        Long startingPrice,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime endTime
) {
}
