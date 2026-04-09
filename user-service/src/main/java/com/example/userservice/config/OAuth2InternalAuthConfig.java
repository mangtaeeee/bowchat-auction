package com.example.userservice.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;

@Configuration
@EnableConfigurationProperties(OAuth2InternalClientProperties.class)
public class OAuth2InternalAuthConfig {

    @Bean
    @ConditionalOnProperty(prefix = "oauth2.internal-client", name = "issuer-uri")
    public JwtDecoder jwtDecoder(OAuth2InternalClientProperties properties) {
        return JwtDecoders.fromIssuerLocation(properties.getIssuerUri());
    }
}
