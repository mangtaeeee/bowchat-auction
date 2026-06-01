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

    @Scheduled(fixedDelayString = "${outbox.publish.fixed-delay-ms:1000}")
    @SchedulerLock(
            name = "outbox-publisher",
            lockAtLeastFor = "${outbox.publish.lock-at-least-for:500ms}",
            lockAtMostFor = "${outbox.publish.lock-at-most-for:10s}"
    )
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents =
                outboxRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc();

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Publishing {} pending outbox events", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                eventProducer.sendSync(
                        event.getTopic(),
                        event.getPartitionKey(),
                        event.getPayload()
                );
                event.markPublished();
            } catch (Exception e) {
                log.error("Outbox publish failed: id={}, error={}", event.getId(), e.getMessage());
            }
        }
    }
}
