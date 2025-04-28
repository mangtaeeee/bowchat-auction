package com.example.bowchat.user.service;

import com.example.bowchat.user.dto.SingUpRequest;
import com.example.bowchat.user.entity.User;
import com.example.bowchat.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void singUn(SingUpRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
        String encodedPassword = passwordEncoder.encode(request.password());
        userRepository.save(User.createLocalUserFromRequest(request,encodedPassword));

    }





}
