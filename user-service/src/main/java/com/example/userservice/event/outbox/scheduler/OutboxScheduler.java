package com.example.userservice.event.outbox.scheduler;

import com.example.userservice.event.outbox.OutboxEvent;
import com.example.userservice.event.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxScheduler {
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    // 1초마다 실행, ShedLock으로 멀티 인스턴스 중복 실행 방지
    @Scheduled(fixedDelay = 1000)
    @SchedulerLock(
            name = "outbox-publisher",
            lockAtLeastFor = "500ms",
            lockAtMostFor = "10s"
    )
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents =
                outboxRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc();

        if (pendingEvents.isEmpty()) return;

        log.debug("발행 대기 이벤트: {}건", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getPartitionKey(), event.getPayload());
                event.markPublished();
                log.info("outbox 발행 완료: topic={}, partitionKey={}", event.getTopic(), event.getPartitionKey());
            } catch (Exception e) {
                // 발행 실패 시 해당 이벤트만 건너뛰고 계속 진행
                // 다음 스케줄러 실행 때 재시도됨
                log.error("outbox 발행 실패: id={}, error={}", event.getId(), e.getMessage());
            }
        }
    }
}
