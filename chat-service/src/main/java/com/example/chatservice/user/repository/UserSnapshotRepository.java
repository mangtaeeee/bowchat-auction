package com.example.chatservice.user.repository;

import com.example.chatservice.user.entity.UserSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSnapshotRepository extends JpaRepository<UserSnapshot, Long> {
}
