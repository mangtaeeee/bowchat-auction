package com.example.bowchat.kafkastarter.producer;

import com.example.bowchat.kafkastarter.event.EventMessage;
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

    private final KafkaTemplate<String, EventMessage> kafkaTemplate;

    // Kafka에 채팅 이벤트를 전송하는 메서드
    public void send(EventMessage chatEvent) {
        String topic = chatEvent.topicName();
        String key   = chatEvent.partitionKey();

        CompletableFuture<SendResult<String, EventMessage>> future =
                kafkaTemplate.send(topic, key, chatEvent);
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
