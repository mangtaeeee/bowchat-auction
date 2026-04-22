package com.example.auctionservice.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

@Schema(description = "경매 시작 요청")
public record StartAuctionRequest(
        @Schema(description = "경매 시작 가격", example = "10000", minimum = "1")
        @NotNull(message = "시작 가격은 필수입니다.")
        @Positive(message = "시작 가격은 0보다 커야 합니다.")
        Long startingPrice,

        @Schema(description = "경매 종료 시각", example = "2099-01-01 10:00:00", type = "string", pattern = "yyyy-MM-dd HH:mm:ss")
        @NotNull(message = "경매 종료 시간은 필수입니다.")
        @Future(message = "경매 종료 시간은 현재보다 이후여야 합니다.")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime endTime
) {
}
