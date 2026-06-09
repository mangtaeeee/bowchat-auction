package com.example.userservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2InternalAuthConfigTest {

    private final OAuth2InternalAuthConfig config = new OAuth2InternalAuthConfig();

    @Test
    void jwtAuthenticationConverterExtractsScopeAndKeycloakRoles() {
        OAuth2InternalClientProperties properties = new OAuth2InternalClientProperties();
        properties.setClientId("user-service");
        properties.setPrincipalAttribute("preferred_username");

        Converter<Jwt, ? extends AbstractAuthenticationToken> converter =
                config.jwtAuthenticationConverter(properties);

        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "RS256"),
                Map.of(
                        "scope", "user.internal.read",
                        "preferred_username", "auction-service",
                        "realm_access", Map.of("roles", List.of("internal_service")),
                        "resource_access", Map.of(
                                "user-service", Map.of("roles", List.of("manage-users"))
                        )
                )
        );

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("auction-service");
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .contains(
                        "SCOPE_user.internal.read",
                        "ROLE_INTERNAL_SERVICE",
                        "ROLE_MANAGE_USERS"
                );
    }

    @Test
    void jwtAuthenticationConverterIgnoresMissingClientRoleBlock() {
        OAuth2InternalClientProperties properties = new OAuth2InternalClientProperties();
        properties.setClientId("user-service");

        Converter<Jwt, ? extends AbstractAuthenticationToken> converter =
                config.jwtAuthenticationConverter(properties);

        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "RS256"),
                Map.of("scope", "user.internal.read")
        );

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("SCOPE_user.internal.read");
    }
}

