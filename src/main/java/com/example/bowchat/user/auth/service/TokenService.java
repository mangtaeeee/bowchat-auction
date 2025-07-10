package com.example.bowchat.user.auth.service;

import com.example.bowchat.user.auth.dto.AuthResponse;
import com.example.bowchat.user.auth.jwt.JwtProvider;
import com.example.bowchat.user.dto.UserInfo;
import com.example.bowchat.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    public AuthResponse issueTokens(User user) {
        String accessToken = jwtProvider.generateToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user.getEmail());

        refreshTokenService.save(
                user.getEmail(),
                refreshToken,
                jwtProvider.getRefreshTokenExpiration()
        );


        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userInfo(UserInfo.of(user))
                .build();
    }
}
