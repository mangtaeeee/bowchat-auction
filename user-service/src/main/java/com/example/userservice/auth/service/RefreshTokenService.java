package com.example.userservice.auth.service;

import com.example.userservice.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
        String refreshToken = refreshTokenRepository.findByEmail(email);
        if (refreshToken == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 세션이 만료되었습니다.");
        }
        return refreshToken;
    }

    public String findEmailByRefreshToken(String refreshToken) {
        String email = refreshTokenRepository.findEmailByRefreshToken(refreshToken);
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 세션이 만료되었습니다.");
        }
        return email;
    }

    public void delete(String email) {
        log.debug("리프레시 토큰 삭제: {}", email);
        refreshTokenRepository.delete(email);
    }

    public void replace(String email, String oldRefreshToken, String newRefreshToken, long expiration) {
        log.debug("리프레시 토큰 교체: {}", email);
        refreshTokenRepository.replace(email, oldRefreshToken, newRefreshToken, expiration);
    }

    public boolean exists(String email) {
        return refreshTokenRepository.exists(email);
    }
}
