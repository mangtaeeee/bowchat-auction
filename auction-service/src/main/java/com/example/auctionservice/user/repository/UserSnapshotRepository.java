package com.example.auctionservice.user.repository;

import com.example.auctionservice.user.entity.UserSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSnapshotRepository extends JpaRepository<UserSnapshot, Long> {
}
