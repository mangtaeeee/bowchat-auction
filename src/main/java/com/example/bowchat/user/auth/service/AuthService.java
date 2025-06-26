package com.example.bowchat.user.auth.service;

import com.example.bowchat.config.jwt.JwtProvider;
import com.example.bowchat.user.auth.dto.AuthResponse;
import com.example.bowchat.user.auth.repository.RefreshTokenRepository;
import com.example.bowchat.user.dto.LoginRequest;
import com.example.bowchat.user.dto.UserInfo;
import com.example.bowchat.user.entity.ProviderType;
import com.example.bowchat.user.entity.User;
import com.example.bowchat.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {


    private final JwtProvider jwtProvider;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthResponse login(LoginRequest loginRequest) {

        log.info("1. 일반 로그인 사용자 조회: {}", loginRequest.email());
        User user = userRepository.findByEmail(loginRequest.email())
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."));

        if (user.getProvider() != ProviderType.LOCAL) {
            log.error("1-2. SNS 로그인 계정으로 일반 로그인 시도: {}", loginRequest.email());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SNS 로그인 계정입니다. 일반 로그인 불가");
        }

        log.info("2. 일반 로그인 사용자 인증");
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.email(),
                        loginRequest.password()
                )
        );

        if (!authentication.isAuthenticated()) {
            log.error("2-2. 일반 로그인 인증 실패: {}", loginRequest.email());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디 혹은 비밀번호가 올바르지 않습니다.");
        }
        log.info("3. 일반 로그인 토큰 생성");
        String token = jwtProvider.generateToken(authentication);
        String refreshToken = jwtProvider.generateRefreshToken(user.getEmail());

        log.info("4. 일반 로그인 리프레시 토큰 저장");
        refreshTokenRepository.save(loginRequest.email(), refreshToken, jwtProvider.getRefreshTokenExpiration());

        return AuthResponse.builder()
                .accessToken(token)
                .refreshToken(refreshToken)
                .userInfo(UserInfo.of(user))
                .build();
    }
}
