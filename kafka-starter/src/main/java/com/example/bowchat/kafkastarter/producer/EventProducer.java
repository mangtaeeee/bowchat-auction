package com.example.bowchat.kafkastarter.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.TimeoutException;
import org.springframework.kafka.core.KafkaTemplate;

@RequiredArgsConstructor
@Slf4j
public class EventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // 비동기 - 일반 이벤트 발행용
    public void send(String topic, String key, Object payload) {
        kafkaTemplate.send(topic, key, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Kafka 전송 실패: topic={}, error={}", topic, ex.getMessage());
                    } else {
                        log.info("Kafka 전송 성공: topic={}, partition={}, offset={}",
                                topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    // 동기 - outbox 스케줄러 전용 (발행 성공 확인 후 markPublished 해야 하므로)
    public void sendSync(String topic, String key, Object payload) {
        try {
            kafkaTemplate.send(topic, key, payload).get(); // 블로킹
            log.info("Kafka 전송 성공: topic={}", topic);
        }  catch (TimeoutException e) {
            log.error("Kafka 전송 타임아웃: topic={}", topic);
            throw new RuntimeException("Kafka 전송 타임아웃", e);
        } catch (Exception e) {
            log.error("Kafka 전송 실패: topic={}, error={}", topic, e.getMessage());
            throw new RuntimeException("Kafka 전송 실패", e);
        }
    }
}