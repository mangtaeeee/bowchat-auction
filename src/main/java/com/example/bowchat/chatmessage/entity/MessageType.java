package com.example.bowchat.chatmessage.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MessageType {

    CHAT("chat-message"),       // 일반 채팅
    ENTER("chat-event"),      // 유저 입장
    LEAVE("chat-event"),      // 유저 퇴장
    SYSTEM("chat-event"),     // 시스템 알림
    AUCTION_BID("auction-bid"),// 경매 입찰
    AUCTION_END("auction-bid"),// 경매 종료
    FILE("chat-message");        // 파일 전송

    private final String topicName; // 카프카 토픽 이름

}
