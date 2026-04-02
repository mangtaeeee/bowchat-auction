package com.example.chatservice.user.service;

import com.example.chatservice.user.client.UserServiceClient;
import com.example.chatservice.user.entity.UserSnapshot;
import com.example.chatservice.user.repository.UserSnapshotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

        // 1. Redis 캐시 조회
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Redis 캐시 히트: userId={}", userId);
            return objectMapper.convertValue(cached, UserSnapshot.class);
        }

        // 2. 로컬 DB(UserSnapshot) 조회
        UserSnapshot local = userSnapshotRepository.findById(userId).orElse(null);
        if (local != null) {
            redisTemplate.opsForValue().set(cacheKey, local, TTL);
            return local;
        }

        // 3. user-service HTTP 호출 (Lazy 동기화)
        try {
            log.info("user-service HTTP 호출: userId={}", userId);
            UserSnapshot snapshot = userServiceClient.getUser(userId);

            // 로컬 DB + Redis에 저장
            userSnapshotSaver.save(snapshot);
            redisTemplate.opsForValue().set(cacheKey, snapshot, TTL);

            return snapshot;

        } catch (FeignException.NotFound e) {
            // user-service가 404 → 진짜 없는 유저
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다.");

        } catch (FeignException e) {
            // user-service 장애 → 서비스 불가
            log.error("user-service 호출 실패: userId={}, error={}", userId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    // user.updated / user.deleted 이벤트 수신 시 캐시 무효화
    public void evictCache(Long userId) {
        redisTemplate.delete(CACHE_PREFIX + userId);
        log.debug("유저 캐시 무효화: userId={}", userId);
    }
}