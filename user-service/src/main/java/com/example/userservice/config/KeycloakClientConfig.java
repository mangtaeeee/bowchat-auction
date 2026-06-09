package com.example.userservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(KeycloakAuthProperties.class)
public class KeycloakClientConfig {

    @Bean
    public RestClient keycloakRestClient() {
        return RestClient.builder().build();
    }
}
