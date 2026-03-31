package com.example.userservice.event.outbox;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "OUTBOX_EVENT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Kafka 토픽
    @Column(nullable = false)
    private String topic;

    // 파티션 키 (userId 등)
    @Column(nullable = false)
    private String partitionKey;

    // JSON 직렬화된 이벤트 페이로드
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    // 발행 여부
    @Column(nullable = false)
    private boolean published;

    // 생성 시각
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 발행 시각
    private LocalDateTime publishedAt;

    public static OutboxEvent create(String topic, String partitionKey, String payload) {
        return OutboxEvent.builder()
                .topic(topic)
                .partitionKey(partitionKey)
                .payload(payload)
                .published(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void markPublished() {
        this.published = true;
        this.publishedAt = LocalDateTime.now();
    }
}
