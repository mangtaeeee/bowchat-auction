package com.example.bowchat.user.service;

import com.example.bowchat.user.auth.dto.AuthResponse;
import com.example.bowchat.user.auth.service.AuthService;
import com.example.bowchat.user.dto.LoginRequest;
import com.example.bowchat.user.dto.SingUpRequest;
import com.example.bowchat.user.entity.PrincipalDetails;
import com.example.bowchat.user.entity.User;
import com.example.bowchat.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthService authService;

    @Test
    void 회원가입() {
        //given
        String email = "test1234@naver.com";
        String password = "1234";
        String nickName = "test1234";
        SingUpRequest request = new SingUpRequest(email, password, nickName);
        //when
        userService.signup(request);

        //then
        assertThat(userRepository.findByEmail(email).isPresent()).isTrue();
    }

    @Test
    void 로그인() {
        // given
        String email = "local@test.com";
        String password = "1234";
        String nickname = "로컬유저";
        SingUpRequest signupRequest = new SingUpRequest(email, password, nickname);
        userService.signup(signupRequest);

        LoginRequest loginRequest = new LoginRequest(email, password);

        // when
        AuthResponse response = authService.login(loginRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isNotBlank();
    }

}