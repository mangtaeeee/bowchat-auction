package com.example.auctionservice.entity;

import lombok.Getter;
import org.springframework.web.server.ResponseStatusException;

@Getter
public class AuctionException extends ResponseStatusException {

    private final AuctionErrorCode errorCode;

    public AuctionException(AuctionErrorCode errorCode) {
        super(errorCode.getStatus(), errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
