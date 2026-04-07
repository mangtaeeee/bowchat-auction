package com.example.auctionservice.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record StartAuctionRequest(
        @NotNull(message = "시작 가격은 필수입니다.")
        @Positive(message = "시작 가격은 0보다 커야 합니다.")
        Long startingPrice,

        @NotNull(message = "경매 종료 시간은 필수입니다.")
        @Future(message = "경매 종료 시간은 현재보다 이후여야 합니다.")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime endTime
) {
}
