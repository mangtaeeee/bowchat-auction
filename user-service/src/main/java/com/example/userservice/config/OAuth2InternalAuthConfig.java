package com.example.userservice.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableConfigurationProperties(OAuth2InternalClientProperties.class)
public class OAuth2InternalAuthConfig {

    @Bean
    @ConditionalOnProperty(prefix = "oauth2.internal-client", name = "issuer-uri")
    @Qualifier("internalJwtDecoder")
    public JwtDecoder jwtDecoder(OAuth2InternalClientProperties properties) {
        return JwtDecoders.fromIssuerLocation(properties.getIssuerUri());
    }

    @Bean
    @ConditionalOnProperty(prefix = "oauth2.internal-client", name = "issuer-uri")
    @Qualifier("internalJwtAuthenticationConverter")
    public Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter(
            OAuth2InternalClientProperties properties
    ) {
        JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setPrincipalClaimName(properties.getPrincipalAttribute());
        authenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Set<GrantedAuthority> authorities = new LinkedHashSet<>(scopeConverter.convert(jwt));
            authorities.addAll(extractRealmRoles(jwt));
            authorities.addAll(extractResourceRoles(jwt, properties.getClientId()));
            return authorities;
        });
        return authenticationConverter;
    }

    private Collection<SimpleGrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) {
            return List.of();
        }

        Object roles = realmAccess.get("roles");
        if (!(roles instanceof Collection<?> roleNames)) {
            return List.of();
        }

        return roleNames.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(this::toRoleAuthority)
                .toList();
    }

    private Collection<SimpleGrantedAuthority> extractResourceRoles(Jwt jwt, String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return List.of();
        }

        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess == null) {
            return List.of();
        }

        Object clientAccess = resourceAccess.get(clientId);
        if (!(clientAccess instanceof Map<?, ?> accessMap)) {
            return List.of();
        }

        Object roles = accessMap.get("roles");
        if (!(roles instanceof Collection<?> roleNames)) {
            return List.of();
        }

        return roleNames.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(this::toRoleAuthority)
                .toList();
    }

    private SimpleGrantedAuthority toRoleAuthority(String role) {
        String normalizedRole = role.toUpperCase().replace('-', '_');
        return new SimpleGrantedAuthority("ROLE_" + normalizedRole);
    }
}
