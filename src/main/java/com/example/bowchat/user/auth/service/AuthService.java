package com.example.bowchat.user.auth.service;

import com.example.bowchat.config.jwt.JwtProvider;
import com.example.bowchat.user.auth.dto.AuthResponse;
import com.example.bowchat.user.dto.LoginRequest;
import com.example.bowchat.user.entity.User;
import com.example.bowchat.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {


    private final JwtProvider jwtProvider;
    private final AuthenticationManager authenticationManager;

    public AuthResponse login(LoginRequest loginRequest) {
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
