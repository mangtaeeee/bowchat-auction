package com.example.bowchat.chatmessage.entity;

public enum MessageType {

    CHAT,       // 일반 채팅
    ENTER,      // 유저 입장
    LEAVE,      // 유저 퇴장
    SYSTEM,     // 시스템 알림
    AUCTION_BID,// 경매 입찰
    AUCTION_END,// 경매 종료
    FILE        // 파일 전송
}
