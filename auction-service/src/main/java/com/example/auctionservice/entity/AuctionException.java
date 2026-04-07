package com.example.auctionservice.entity;

import lombok.Getter;

@Getter
public class AuctionException extends RuntimeException {

    private final AuctionErrorCode errorCode;

    public AuctionException(AuctionErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
