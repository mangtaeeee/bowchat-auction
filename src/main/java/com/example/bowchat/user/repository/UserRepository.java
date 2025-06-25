package com.example.bowchat.user.repository;

import com.example.bowchat.user.entity.ProviderType;
import com.example.bowchat.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByEmailAndProvider(String email, ProviderType provider);
}
