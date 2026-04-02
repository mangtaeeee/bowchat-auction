package com.example.productservice.user.repository;

import com.example.productservice.user.entity.UserSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSnapshotRepository extends JpaRepository<UserSnapshot, Long> {
}
