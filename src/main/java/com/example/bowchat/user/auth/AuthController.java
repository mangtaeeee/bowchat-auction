package com.example.bowchat.user.auth;

import com.example.bowchat.user.auth.dto.AccessTokenResponse;
import com.example.bowchat.user.auth.dto.AuthResponse;
import com.example.bowchat.user.auth.service.AuthService;
import com.example.bowchat.user.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<AccessTokenResponse> refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
        String newAccessToken = authService.refreshAccessToken(request);

        response.setHeader("Authorization", "Bearer " + newAccessToken);

        return ResponseEntity.ok(new AccessTokenResponse(newAccessToken));
    }


}
