package com.example.auctionservice.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        @DefaultValue("http://localhost:5173")
        List<String> allowedOrigins,
        @DefaultValue({"GET", "POST", "PUT", "DELETE", "OPTIONS"})
        List<String> allowedMethods,
        @DefaultValue("*")
        List<String> allowedHeaders,
        @DefaultValue("true")
        boolean allowCredentials
) {
}
