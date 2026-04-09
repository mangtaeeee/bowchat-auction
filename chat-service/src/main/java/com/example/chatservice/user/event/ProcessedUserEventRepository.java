package com.example.chatservice.user.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface ProcessedUserEventRepository extends JpaRepository<ProcessedUserEvent, String> {

    @Modifying
    @Query(value = """
            INSERT INTO processed_user_events (event_id, event_type, aggregate_id, processed_at)
            VALUES (:eventId, :eventType, :aggregateId, :processedAt)
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("eventId") String eventId,
            @Param("eventType") String eventType,
            @Param("aggregateId") Long aggregateId,
            @Param("processedAt") Instant processedAt
    );
}
