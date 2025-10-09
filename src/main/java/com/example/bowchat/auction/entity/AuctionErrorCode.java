package com.example.bowchat.auction.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum AuctionErrorCode {
    // 경매 관련
    AUCTION_NOT_FOUND(HttpStatus.NOT_FOUND, "경매를 찾을 수 없습니다."),
    BID_TOO_LOW(HttpStatus.BAD_REQUEST, "입찰 금액이 현재가보다 낮습니다."),
    SELLER_CANNOT_BID(HttpStatus.BAD_REQUEST, "판매자는 자신의 상품에 입찰할 수 없습니다."),
    AUCTION_CLOSED(HttpStatus.BAD_REQUEST, "이미 종료된 경매입니다.");

    private final HttpStatus status;
    private final String message;

    public String getCode() {
        return this.name();
    }
    public int getStatusCode() { return status.value(); }     // 400
}
