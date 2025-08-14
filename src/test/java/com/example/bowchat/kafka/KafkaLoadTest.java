package com.example.bowchat.kafka;

import com.example.bowchat.chatmessage.entity.MessageType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

@SpringBootTest(
        properties = {
                // point Spring’s Kafka client to the embedded broker
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
        }
)
@EmbeddedKafka(
        partitions   = 1,
        topics       = {"chat-message"},
        // write the embedded broker’s address into this property
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@ActiveProfiles("dev")
public class KafkaLoadTest {
    @Autowired
    private KafkaTemplate<String, ChatEvent> kafkaTemplate;

    @Test
    void send10000MessagesPerSecond() throws Exception {
        int totalMessages = 10000;
        long start = System.currentTimeMillis();

        for (int i = 0; i < totalMessages; i++) {
            ChatEvent event = new ChatEvent(
                    1L,
                    (long)i,
                    "User" + i,
                    MessageType.CHAT,
                    "테스트 메시지 " + i,
                    Instant.now().toEpochMilli()
            );
            kafkaTemplate.send("chat-message", event);
        }

        long end = System.currentTimeMillis();
        System.out.println("총 처리 시간(ms): " + (end - start));
    }
}
