package com.example.bowchat.user.auth.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String PREFIX = "refresh_token:";

    // 토큰 저장
    public void save(String email, String refreshToken, long expiration) {
        redisTemplate.opsForValue().set(PREFIX + email, refreshToken, Duration.ofMillis(expiration));
    }

    // 토큰 조회
    public String findByEmail(String email) {
        return redisTemplate.opsForValue().get(PREFIX + email);
    }

    // 토큰 삭제
    public void delete(String email) {
        redisTemplate.delete(PREFIX + email);
    }

    // 존재 여부
    public boolean exists(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + email));
    }
}
