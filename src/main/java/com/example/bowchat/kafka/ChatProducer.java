package com.example.bowchat.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class ChatProducer {

    private final KafkaTemplate<String, ChatEvent> kafkaTemplate;

    /**
     * 채팅 이벤트를 카프카 토픽에 전송하는 메서드
     *
     * @param chatEvent 전송할 채팅 이벤트
     */
    public void send(ChatEvent chatEvent) {
        kafkaTemplate.send("chat-topic", chatEvent);
    }
}
