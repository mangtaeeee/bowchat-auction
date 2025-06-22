package com.example.bowchat.user.auth.service;

import com.example.bowchat.config.jwt.JwtProvider;
import com.example.bowchat.user.auth.dto.AuthResponse;
import com.example.bowchat.user.dto.LoginRequest;
import com.example.bowchat.user.entity.ProviderType;
import com.example.bowchat.user.entity.User;
import com.example.bowchat.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
public class AuthService {


    private final JwtProvider jwtProvider;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    public AuthResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.email())
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."));

        if (user.getProvider() != ProviderType.LOCAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SNS 로그인 계정입니다. 일반 로그인 불가");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.email(),
                        loginRequest.password()
                )
        );

        String token = jwtProvider.generateToken(authentication);
        return AuthResponse.builder()
                .accessToken(token)
                .build();
    }
}
