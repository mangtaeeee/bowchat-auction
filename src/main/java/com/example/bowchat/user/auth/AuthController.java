package com.example.bowchat.user.auth;

import com.example.bowchat.user.auth.dto.AuthResponse;
import com.example.bowchat.user.auth.service.AuthService;
import com.example.bowchat.user.dto.LoginRequest;
import com.example.bowchat.user.dto.UserResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/login")
    public ResponseEntity<UserResponse> login(
            @RequestBody LoginRequest loginRequest,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.login(loginRequest);
        response.setHeader("Authorization", "Bearer " + authResponse.accessToken());

        Cookie refreshTokenCookie = new Cookie("refreshToken", authResponse.refreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(refreshTokenCookie);


        return ResponseEntity.ok(new UserResponse(authResponse.userInfo()));
    }

}
