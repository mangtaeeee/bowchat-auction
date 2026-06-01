package com.example.chatservice.auth.config;

import com.example.chatservice.auth.AuthConstants;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

import java.util.Optional;

@Configuration
public class FeignConfig {

    @Value("${internal.secret:}")
    private String internalSecret;

    @Value("${oauth2.internal-client.registration-id:}")
    private String clientRegistrationId;

    @Bean
    public RequestInterceptor internalAuthenticationInterceptor(Optional<OAuth2AuthorizedClientManager> authorizedClientManager) {
        return requestTemplate -> {
            if (!internalSecret.isBlank()) {
                requestTemplate.header(AuthConstants.INTERNAL_TOKEN_HEADER, internalSecret);
            }

            authorizedClientManager
                    .map(manager -> OAuth2ClientConfig.resolveAccessToken(manager, clientRegistrationId))
                    .filter(token -> !token.isBlank())
                    .ifPresent(token -> requestTemplate.header(HttpHeaders.AUTHORIZATION, AuthConstants.BEARER_PREFIX + token));
        };
    }
}
