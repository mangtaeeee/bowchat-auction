package com.example.chatservice.user.client;

import com.example.chatservice.user.entity.UserSnapshot;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "user-service",
        url = "${user-service.url}"
)
public interface UserServiceClient {

    @GetMapping("/internal/users/{userId}")
    UserSnapshot getUser(@PathVariable Long userId);
}
