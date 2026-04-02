package com.example.chatservice.auth.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Value("${internal.secret}")
    private String internalSecret;

    // 모든 FeignClient 요청에 X-Service-Token 헤더 자동 추가
    @Bean
    public RequestInterceptor internalTokenInterceptor() {
        return requestTemplate ->
                requestTemplate.header("X-Service-Token", internalSecret);
    }
}
