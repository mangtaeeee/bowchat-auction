package com.example.bowchat.auction.entity;

import com.example.bowchat.global.exception.BowChatException;
import org.springframework.http.HttpStatus;


public class AuctionException extends BowChatException {

    private final AuctionErrorCode errorCode;

    public AuctionException(AuctionErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    @Override
    public HttpStatus getStatus() {
        return errorCode.getStatus();
    }

    @Override
    public String getCode() {
        return errorCode.getCode();
    }
}
