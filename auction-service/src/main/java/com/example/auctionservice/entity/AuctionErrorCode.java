package com.example.auctionservice.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum AuctionErrorCode {
    AUCTION_NOT_FOUND(HttpStatus.NOT_FOUND, "경매를 찾을 수 없습니다."),
    AUCTION_NOT_FOUND_BY_PRODUCT(HttpStatus.NOT_FOUND, "해당 상품의 경매를 찾을 수 없습니다."),
    BID_TOO_LOW(HttpStatus.BAD_REQUEST, "입찰 금액은 현재가보다 높아야 합니다."),
    SELLER_CANNOT_BID(HttpStatus.BAD_REQUEST, "판매자는 자신의 상품에 입찰할 수 없습니다."),
    AUCTION_CLOSED(HttpStatus.BAD_REQUEST, "이미 종료된 경매입니다."),
    CONCURRENT_BID_CONFLICT(HttpStatus.CONFLICT, "동시에 더 높은 입찰이 반영되었습니다. 다시 시도해 주세요."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 상품입니다."),
    PRODUCT_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "상품 서비스에 접근할 수 없습니다."),
    ONLY_SELLER_CAN_START_AUCTION(HttpStatus.FORBIDDEN, "상품 판매자만 경매를 시작할 수 있습니다."),
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
