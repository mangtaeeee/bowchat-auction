package com.example.bowchat.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;


@Service
@RequiredArgsConstructor
@Slf4j
public class ChatProducer {

    private final KafkaTemplate<String, ChatEvent> kafkaTemplate;

    /**
     * 채팅 이벤트를 카프카 토픽에 전송하는 메서드
     *
     * @param chatEvent 전송할 채팅 이벤트
     */
    public void send(ChatEvent chatEvent) {
        String topic = chatEvent.type().getTopicName();

        CompletableFuture<SendResult<String, ChatEvent>> future = kafkaTemplate.send(topic, chatEvent);
        future.whenComplete((stringChatEventSendResult, throwable) ->
        {
            if (throwable != null) {
                log.error("Kafka 전송 실패: topic={}, error={}", topic, throwable.getMessage());
            } else {
                log.info("Kafka 전송 성공: topic={}, partition={}, offset={}",
                        topic,
                        stringChatEventSendResult.getRecordMetadata().partition(),
                        stringChatEventSendResult.getRecordMetadata().offset());
            }
        });

    }

}
