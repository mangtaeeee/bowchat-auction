package com.example.userservice.auth.service;

import com.example.userservice.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

//Redis 議고쉶
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public void save(String email, String refreshToken, long expiration) {
        log.debug("由ы봽?덉떆 ?좏겙 ??? {}", email);
        refreshTokenRepository.save(email, refreshToken, expiration);
    }

    public String findRefreshTokenByEmail(String email) {
        return Optional.ofNullable(refreshTokenRepository.findByEmail(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "濡쒓렇???몄뀡??留뚮즺?섏뿀?듬땲??"));
    }

    public void delete(String email) {
        log.debug("由ы봽?덉떆 ?좏겙 ??젣: {}", email);
        refreshTokenRepository.delete(email);
    }

    public boolean exists(String email) {
        return refreshTokenRepository.exists(email);
    }
}
