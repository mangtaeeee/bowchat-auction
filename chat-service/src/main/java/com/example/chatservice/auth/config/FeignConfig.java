package com.example.chatservice.auth.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor internalAuthenticationInterceptor(
            OAuth2AuthorizedClientManager authorizedClientManager,
            OAuth2InternalClientProperties properties
    ) {
        return requestTemplate -> {
            String token = OAuth2ClientConfig.resolveAccessToken(
                    authorizedClientManager,
                    properties.getRegistrationId()
            );
            requestTemplate.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        };
    }
}
