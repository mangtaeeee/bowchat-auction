package com.example.bowchat.user.service;

import com.example.bowchat.user.dto.SingUpRequest;
import com.example.bowchat.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UserServiceTest {

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

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
}