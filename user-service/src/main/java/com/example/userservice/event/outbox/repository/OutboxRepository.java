package com.example.userservice.event.outbox.repository;

import com.example.userservice.event.outbox.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    // 아직 발행 안 된 이벤트 조회 (스케줄러에서 사용)
    List<OutboxEvent> findTop100ByPublishedFalseOrderByCreatedAtAsc();
}
