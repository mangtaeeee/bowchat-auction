package com.example.bowchat.user.controller;

import com.example.bowchat.user.dto.SingUpRequest;
import com.example.bowchat.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/auth/signup")
    public void signup(@RequestBody SingUpRequest request) {
        userService.singUn(request);
    }

}
