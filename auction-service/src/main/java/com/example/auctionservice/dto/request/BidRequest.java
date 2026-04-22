package com.example.auctionservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "입찰 요청")
public record BidRequest(
        @Schema(description = "입찰 금액", example = "15000", minimum = "1")
        @NotNull(message = "입찰 금액은 필수입니다.")
        @Positive(message = "입찰 금액은 0보다 커야 합니다.")
        Long bidAmount
) {
}
