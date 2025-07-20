package com.example.bowchat.chatmessage.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("dev") // dev 프로필에서만 동작
@RequiredArgsConstructor
@Slf4j
public class DevDataCleaner {

    private final ChatMessageRepository chatMessageRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void cleanData() {
        log.info("개발 환경에서 데이터 초기화");
        chatMessageRepository.deleteAll(); // 모든 채팅 메시지 삭제
        log.info("채팅 메시지 데이터 초기화 완료");
    }
}
