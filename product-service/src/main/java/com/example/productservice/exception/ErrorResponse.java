package com.example.productservice.exception;


import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String message,
        Map<String, Object> context  // 어떤 ID가 문제인지 등 컨텍스트 정보
) {
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, null);
    }

    public static ErrorResponse of(String code, String message, Map<String, Object> context) {
        return new ErrorResponse(code, message, context);
    }
}
