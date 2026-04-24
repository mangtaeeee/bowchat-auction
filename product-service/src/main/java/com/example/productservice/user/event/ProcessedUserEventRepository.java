package com.example.productservice.user.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface ProcessedUserEventRepository extends JpaRepository<ProcessedUserEvent, String> {

    @Modifying
    @Query(value = """
            INSERT INTO processed_user_events (eventId, eventType, aggregateId, processedAt)
            VALUES (:eventId, :eventType, :aggregateId, :processedAt)
            ON CONFLICT (eventId) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("eventId") String eventId,
            @Param("eventType") String eventType,
            @Param("aggregateId") Long aggregateId,
            @Param("processedAt") Instant processedAt
    );
}
