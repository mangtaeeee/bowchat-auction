package com.example.chatservice.user.service;

import com.example.chatservice.exception.ChatErrorCode;
import com.example.chatservice.exception.ChatException;
import com.example.chatservice.user.client.UserServiceClient;
import com.example.chatservice.user.entity.UserSnapshot;
import com.example.chatservice.user.repository.UserSnapshotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserQueryService {

    private final UserServiceClient userServiceClient;
    private final UserSnapshotRepository userSnapshotRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserSnapshotSaver userSnapshotSaver;

    private static final String CACHE_PREFIX = "chat:user:";
    private static final Duration TTL = Duration.ofMinutes(10);
    private final ObjectMapper objectMapper;

    public UserSnapshot getUser(Long userId) {
        String cacheKey = CACHE_PREFIX + userId;

        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Redis 캐시 히트: userId={}", userId);
            return objectMapper.convertValue(cached, UserSnapshot.class);
        }

        UserSnapshot local = userSnapshotRepository.findById(userId).orElse(null);
        if (local != null) {
            redisTemplate.opsForValue().set(cacheKey, local, TTL);
            return local;
        }

        try {
            log.info("user-service HTTP 호출: userId={}", userId);
            UserSnapshot snapshot = userServiceClient.getUser(userId);

            userSnapshotSaver.save(snapshot);
            redisTemplate.opsForValue().set(cacheKey, snapshot, TTL);

            return snapshot;
        } catch (FeignException.NotFound e) {
            throw new ChatException(ChatErrorCode.USER_NOT_FOUND);
        } catch (FeignException e) {
            log.error("user-service 호출 실패: userId={}, error={}", userId, e.getMessage());
            throw new ChatException(ChatErrorCode.USER_SERVICE_UNAVAILABLE);
        }
    }

    public void evictCache(Long userId) {
        redisTemplate.delete(CACHE_PREFIX + userId);
        log.debug("유저 캐시 무효화: userId={}", userId);
    }
}
