package com.example.auctionservice.user.repository;

import com.example.auctionservice.user.entity.UserSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSnapshotRepository extends JpaRepository<UserSnapshot, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO user_snapshots (userid, email, nickname)
            VALUES (:userId, :email, :nickname)
            ON CONFLICT (userid) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("userId") Long userId,
            @Param("email") String email,
            @Param("nickname") String nickname
    );
}
