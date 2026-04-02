package com.example.userservice.controller;

import com.example.userservice.dto.request.UserSnapshot;
import com.example.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
@Slf4j
public class InternalUserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<UserSnapshot> getUser(@PathVariable Long userId) {
        log.debug("내부 유저 조회: userId={}", userId);
        return ResponseEntity.ok(userService.getUser(userId));
    }
}
