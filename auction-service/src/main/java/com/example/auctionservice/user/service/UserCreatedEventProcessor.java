package com.example.auctionservice.user.service;

import com.example.auctionservice.user.event.ProcessedUserEventRepository;
import com.example.auctionservice.user.event.UserCreatedEvent;
import com.example.auctionservice.user.repository.UserSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCreatedEventProcessor {

    private static final String USER_CREATED_EVENT = "user.created";

    private final ProcessedUserEventRepository processedUserEventRepository;
    private final UserSnapshotRepository userSnapshotRepository;

    @Transactional
    public boolean process(UserCreatedEvent event) {
        int inserted = processedUserEventRepository.insertIfAbsent(
                event.eventId(),
                USER_CREATED_EVENT,
                event.userId(),
                event.occurredAt()
        );

        if (inserted == 0) {
            log.debug("Duplicate {} skipped: eventId={}", USER_CREATED_EVENT, event.eventId());
            return false;
        }

        userSnapshotRepository.insertIfAbsent(
                event.userId(),
                event.email(),
                event.nickName()
        );
        return true;
    }
}
