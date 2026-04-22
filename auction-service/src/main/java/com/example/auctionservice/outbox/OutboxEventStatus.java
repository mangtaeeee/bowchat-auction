package com.example.auctionservice.outbox;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum OutboxEventStatus {


    PENDING("이벤트가 생성되었지만 아직 처리되지 않은 상태"),
    PROCESSING("이벤트가 현재 처리 중인 상태"),
    PUBLISHED("이벤트가 성공적으로 처리된 상태"),;

    private final String description;

}
