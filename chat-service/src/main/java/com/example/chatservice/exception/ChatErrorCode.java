package com.example.chatservice.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum ChatErrorCode {
    UNSUPPORTED_CHAT_ROOM_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 채팅방 타입입니다."),
    AUCTION_NOT_FOUND(HttpStatus.NOT_FOUND, "진행 중인 경매를 찾을 수 없습니다."),
    AUCTION_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "경매 서비스에 접근할 수 없습니다."),
    AUCTION_CLOSED(HttpStatus.BAD_REQUEST, "종료된 경매입니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 상품입니다."),
    PRODUCT_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "상품 서비스에 접근할 수 없습니다."),
    OWN_PRODUCT_CHAT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "본인 상품에는 채팅방을 생성할 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    USER_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "일시적으로 사용자 정보를 조회할 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    public String getCode() {
        return name();
    }

    public int getStatusCode() {
        return status.value();
    }
}
