package com.example.auctionservice.user.service;

import com.example.auctionservice.user.entity.UserSnapshot;
import com.example.auctionservice.user.repository.UserSnapshotRepository;
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
    public void save(UserSnapshot snapshot)
    {
        userSnapshotRepository.save(snapshot);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveIfAbsent(UserSnapshot snapshot) {
        int inserted = userSnapshotRepository.insertIfAbsent(
                snapshot.getUserId(),
                snapshot.getEmail(),
                snapshot.getNickname()
        );

        if (inserted > 0) {
            log.debug("저장 userId={}", snapshot.getUserId());
        } else {
            log.debug("이미 존재하는 userId={}", snapshot.getUserId());
        }
    }
}
