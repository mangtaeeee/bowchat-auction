package com.example.auctionservice.user.service;

import com.example.auctionservice.entity.AuctionErrorCode;
import com.example.auctionservice.entity.AuctionException;
import com.example.auctionservice.user.client.UserServiceClient;
import com.example.auctionservice.user.entity.UserSnapshot;
import com.example.auctionservice.user.repository.UserSnapshotRepository;
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

    private static final String CACHE_PREFIX = "auction:user:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final UserServiceClient userServiceClient;
    private final UserSnapshotRepository userSnapshotRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserSnapshotSaver userSnapshotSaver;
    private final ObjectMapper objectMapper;

    public UserSnapshot getUser(Long userId) {
        String cacheKey = CACHE_PREFIX + userId;

        UserSnapshot cachedUser = getCachedUser(cacheKey, userId);
        if (cachedUser != null) {
            return cachedUser;
        }

        UserSnapshot localSnapshot = userSnapshotRepository.findById(userId).orElse(null);
        if (localSnapshot != null) {
            cacheUser(cacheKey, localSnapshot);
            return localSnapshot;
        }

        return fetchFromUserService(cacheKey, userId);
    }

    public void evictCache(Long userId) {
        redisTemplate.delete(CACHE_PREFIX + userId);
        log.debug("유저 캐시 무효화: userId={}", userId);
    }

    private UserSnapshot getCachedUser(String cacheKey, Long userId) {
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached == null) {
            return null;
        }

        log.debug("Redis 캐시 히트: userId={}", userId);
        return objectMapper.convertValue(cached, UserSnapshot.class);
    }

    private UserSnapshot fetchFromUserService(String cacheKey, Long userId) {
        try {
            log.info("user-service HTTP 호출: userId={}", userId);
            UserSnapshot snapshot = userServiceClient.getUser(userId);
            userSnapshotSaver.save(snapshot);
            cacheUser(cacheKey, snapshot);
            return snapshot;
        } catch (FeignException.NotFound e) {
            throw new AuctionException(AuctionErrorCode.USER_NOT_FOUND);
        } catch (FeignException e) {
            log.error("user-service 호출 실패: userId={}, status={}", userId, e.status(), e);
            throw new AuctionException(AuctionErrorCode.USER_SERVICE_UNAVAILABLE);
        }
    }

    private void cacheUser(String cacheKey, UserSnapshot snapshot) {
        redisTemplate.opsForValue().set(cacheKey, snapshot, TTL);
    }
}
