package com.example.userservice.auth.repository;

import com.example.userservice.auth.AuthConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private final StringRedisTemplate redisTemplate;

    // ?좏겙 ???
    public void save(String email, String refreshToken, long expiration) {
        redisTemplate.opsForValue().set(
                AuthConstants.REFRESH_TOKEN_REDIS_PREFIX + email,
                refreshToken,
                Duration.ofMillis(expiration)
        );
    }

    // ?좏겙 議고쉶
    public String findByEmail(String email) {
        return redisTemplate.opsForValue().get(AuthConstants.REFRESH_TOKEN_REDIS_PREFIX + email);
    }

    // ?좏겙 ??젣
    public void delete(String email) {
        redisTemplate.delete(AuthConstants.REFRESH_TOKEN_REDIS_PREFIX + email);
    }

    // 議댁옱 ?щ?
    public boolean exists(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(AuthConstants.REFRESH_TOKEN_REDIS_PREFIX + email));
    }
}
