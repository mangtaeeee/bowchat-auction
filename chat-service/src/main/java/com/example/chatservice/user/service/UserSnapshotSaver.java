package com.example.chatservice.user.service;

import com.example.chatservice.user.entity.UserSnapshot;
import com.example.chatservice.user.repository.UserSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveIfAbsent(UserSnapshot snapshot) {
        try {
            userSnapshotRepository.save(snapshot);
            log.debug("UserSnapshot saved: userId={}", snapshot.getUserId());
        } catch (DataIntegrityViolationException e) {
            log.debug("UserSnapshot already exists: userId={}", snapshot.getUserId());
        }
    }
}
