package com.example.auctionservice.outbox;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OutboxConstants {

    public static final int LAST_ERROR_MAX_LENGTH = 500;
}
