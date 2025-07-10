package com.example.bowchat.user.auth.service;

import com.example.bowchat.user.auth.dto.AuthResponse;
import com.example.bowchat.user.auth.jwt.JwtProvider;
import com.example.bowchat.user.dto.LoginRequest;
import com.example.bowchat.user.entity.PrincipalDetails;
import com.example.bowchat.user.entity.User;
import com.example.bowchat.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {


    private final JwtProvider jwtProvider;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final TokenService tokenService;

    public AuthResponse login(LoginRequest loginRequest) {
        log.info("CustomAuthenticationProvider로 인증 시도");

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.email(),
                        loginRequest.password()
                )
        );

        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        User user = principalDetails.getUser();

        return tokenService.issueTokens(user);
    }

    public String refreshAccessToken(HttpServletRequest request) {
        String refreshToken = tokenService.extractRefreshToken(request);

        tokenService.validateRefreshToken(refreshToken);

        String email = jwtProvider.getEmailFromToken(refreshToken);

        tokenService.verifyRefreshTokenInRedis(email, refreshToken);

        User user = loadUser(email);

        return tokenService.issueNewAccessToken(user);
    }


    private User loadUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

}
