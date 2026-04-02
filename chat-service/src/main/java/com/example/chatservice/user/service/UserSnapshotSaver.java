package com.example.chatservice.user.service;

import com.example.chatservice.user.entity.UserSnapshot;
import com.example.chatservice.user.repository.UserSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserSnapshotSaver {

    private final UserSnapshotRepository userSnapshotRepository;

    @Transactional
    public void save(UserSnapshot snapshot) {
        userSnapshotRepository.save(snapshot);
    }

    // UserEventConsumer에서 사용 - 조회+저장 원자적으로 처리 (멱등성 보장)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveIfAbsent(UserSnapshot snapshot) {
        if (!userSnapshotRepository.existsById(snapshot.getUserId())) {
            userSnapshotRepository.save(snapshot);
            log.debug("UserSnapshot 저장: userId={}", snapshot.getUserId());
        } else {
            log.debug("이미 존재하는 UserSnapshot: userId={}", snapshot.getUserId());
        }
    }
}
