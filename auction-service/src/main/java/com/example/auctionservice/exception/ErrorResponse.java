package com.example.auctionservice.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "에러 응답")
public record ErrorResponse(
        @Schema(description = "에러 코드", example = "AUCTION_NOT_FOUND")
        String code,
        @Schema(description = "에러 메시지", example = "경매를 찾을 수 없습니다.")
        String message,
        @Schema(description = "검증 실패 등 추가 정보", nullable = true)
        Map<String, Object> context
) {
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, null);
    }

    public static ErrorResponse of(String code, String message, Map<String, Object> context) {
        return new ErrorResponse(code, message, context);
    }
}
