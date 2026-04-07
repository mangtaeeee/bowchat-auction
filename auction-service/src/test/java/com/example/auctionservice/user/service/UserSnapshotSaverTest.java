package com.example.auctionservice.user.service;

import com.example.auctionservice.user.entity.UserSnapshot;
import com.example.auctionservice.user.repository.UserSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserSnapshotSaverTest {

    @Mock
    private UserSnapshotRepository userSnapshotRepository;

    @Test
    void saveIfAbsentUsesInsertOnConflictPath() {
        // 중복 소비를 견디기 위해 existsById가 아니라 ON CONFLICT 경로를 타는지 확인한다.
        UserSnapshot snapshot = UserSnapshot.builder()
                .userId(1L)
                .email("user@test.com")
                .nickname("tester")
                .build();

        UserSnapshotSaver saver = new UserSnapshotSaver(userSnapshotRepository);
        saver.saveIfAbsent(snapshot);

        verify(userSnapshotRepository).insertIfAbsent(1L, "user@test.com", "tester");
    }
}
