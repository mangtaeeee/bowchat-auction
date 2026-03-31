package com.example.userservice.event.outbox.scheduler;

import com.example.bowchat.kafkastarter.producer.EventProducer;
import com.example.userservice.event.outbox.OutboxEvent;
import com.example.userservice.event.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxScheduler {
    private final OutboxRepository outboxRepository;
    private final EventProducer eventProducer;

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
                eventProducer.sendSync(  // send → sendSync
                        event.getTopic(),
                        event.getPartitionKey(),
                        event.getPayload()
                );
                event.markPublished(); // 발행 성공 확인 후 완료 처리
            } catch (Exception e) {
                log.error("outbox 발행 실패: id={}, error={}", event.getId(), e.getMessage());
                // 예외 던지지 않고 다음 이벤트로 넘어감 (재시도는 다음 스케줄러 실행 때)
            }
        }
    }
}
