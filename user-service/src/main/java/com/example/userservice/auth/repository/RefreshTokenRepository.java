package com.example.userservice.auth.repository;

import com.example.userservice.auth.AuthConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    // Refresh Token으로 로그아웃/재발급 요청이 들어올 수 있어 역방향 인덱스를 같이 유지한다.
    private static final String REFRESH_TOKEN_INDEX_PREFIX = "refresh_token_index:";

    private final StringRedisTemplate redisTemplate;

    public void save(String email, String refreshToken, long expiration) {
        redisTemplate.opsForValue().set(
                AuthConstants.REFRESH_TOKEN_REDIS_PREFIX + email,
                refreshToken,
                Duration.ofMillis(expiration)
        );
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_INDEX_PREFIX + refreshToken,
                email,
                Duration.ofMillis(expiration)
        );
    }

    public String findByEmail(String email) {
        return redisTemplate.opsForValue().get(AuthConstants.REFRESH_TOKEN_REDIS_PREFIX + email);
    }

    public String findEmailByRefreshToken(String refreshToken) {
        return redisTemplate.opsForValue().get(REFRESH_TOKEN_INDEX_PREFIX + refreshToken);
    }

    public void delete(String email) {
        String refreshToken = findByEmail(email);
        redisTemplate.delete(AuthConstants.REFRESH_TOKEN_REDIS_PREFIX + email);
        if (refreshToken != null) {
            redisTemplate.delete(REFRESH_TOKEN_INDEX_PREFIX + refreshToken);
        }
    }

    public void replace(String email, String oldRefreshToken, String newRefreshToken, long expiration) {
        if (oldRefreshToken != null) {
            redisTemplate.delete(REFRESH_TOKEN_INDEX_PREFIX + oldRefreshToken);
        }
        save(email, newRefreshToken, expiration);
    }

    public boolean exists(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(AuthConstants.REFRESH_TOKEN_REDIS_PREFIX + email));
    }
}
