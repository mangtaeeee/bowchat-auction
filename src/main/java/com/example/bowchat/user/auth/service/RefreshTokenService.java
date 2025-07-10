package com.example.bowchat.user.auth.service;

import com.example.bowchat.user.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

//Redis 조회
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public void save(String email, String refreshToken, long expiration) {
        log.debug("리프레시 토큰 저장: {}", email);
        refreshTokenRepository.save(email, refreshToken, expiration);
    }

    public String findRefreshTokenByEmail(String email) {
        return refreshTokenRepository.findByKey("refresh_token:" + email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 세션이 만료되었습니다."));
    }

    public void delete(String email) {
        log.debug("리프레시 토큰 삭제: {}", email);
        refreshTokenRepository.delete(email);
    }

    public boolean exists(String email) {
        return refreshTokenRepository.exists(email);
    }
}
