package com.example.auctionservice.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

    @Query("""
            select e.id
            from OutboxEvent e
            where e.status = :status
              and e.nextAttemptAt <= :now
            order by e.createdAt asc
            """)
    List<String> findProcessableIds(
            @Param("status") OutboxEventStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OutboxEvent e
            set e.status = :processingStatus,
                e.processingStartedAt = :processingStartedAt,
                e.retryCount = e.retryCount + 1,
                e.lastError = null
            where e.id = :id
              and e.status = :pendingStatus
              and e.nextAttemptAt <= :now
            """)
    int claim(
            @Param("id") String id,
            @Param("pendingStatus") OutboxEventStatus pendingStatus,
            @Param("processingStatus") OutboxEventStatus processingStatus,
            @Param("now") LocalDateTime now,
            @Param("processingStartedAt") LocalDateTime processingStartedAt
    );

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OutboxEvent e
            set e.status = :publishedStatus,
                e.processingStartedAt = null,
                e.publishedAt = :publishedAt,
                e.lastError = null
            where e.id = :id
              and e.status = :processingStatus
            """)
    int markPublished(
            @Param("id") String id,
            @Param("processingStatus") OutboxEventStatus processingStatus,
            @Param("publishedStatus") OutboxEventStatus publishedStatus,
            @Param("publishedAt") LocalDateTime publishedAt
    );

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OutboxEvent e
            set e.status = :pendingStatus,
                e.processingStartedAt = null,
                e.nextAttemptAt = :nextAttemptAt,
                e.lastError = :lastError
            where e.id = :id
              and e.status = :processingStatus
            """)
    int reschedule(
            @Param("id") String id,
            @Param("processingStatus") OutboxEventStatus processingStatus,
            @Param("pendingStatus") OutboxEventStatus pendingStatus,
            @Param("nextAttemptAt") LocalDateTime nextAttemptAt,
            @Param("lastError") String lastError
    );

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OutboxEvent e
            set e.status = :pendingStatus,
                e.processingStartedAt = null,
                e.nextAttemptAt = :nextAttemptAt,
                e.lastError = :lastError
            where e.status = :processingStatus
              and e.processingStartedAt < :staleBefore
            """)
    int recoverStuckEvents(
            @Param("processingStatus") OutboxEventStatus processingStatus,
            @Param("pendingStatus") OutboxEventStatus pendingStatus,
            @Param("staleBefore") LocalDateTime staleBefore,
            @Param("nextAttemptAt") LocalDateTime nextAttemptAt,
            @Param("lastError") String lastError
    );
}
