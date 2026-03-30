package com.example.userservice.service;

import com.example.userservice.dto.SingUpRequest;
import com.example.userservice.entity.User;
import com.example.userservice.event.UserCreatedEvent;
import com.example.userservice.event.UserEventPublisher;
import com.example.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserEventPublisher userEventPublisher;

    @Transactional
    public void signup(SingUpRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다.");
        }
        String encodedPassword = passwordEncoder.encode(request.password());
        User user = userRepository.save(User.createLocalUserFromRequest(request, encodedPassword));

        userEventPublisher.publishUserCreatedEvent(
                UserCreatedEvent.of(user)
        );
    }
}