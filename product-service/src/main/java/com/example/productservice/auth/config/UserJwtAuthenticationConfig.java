package com.example.productservice.auth.config;

import com.example.productservice.auth.AuthConstants;
import com.example.productservice.auth.UserPrincipal;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
public class UserJwtAuthenticationConfig {

    @Bean
    @ConditionalOnProperty(prefix = "oauth2.internal-client", name = "issuer-uri")
    public Converter<Jwt, AbstractAuthenticationToken> userJwtAuthenticationConverter() {
        return new Converter<>() {
            @Override
            public AbstractAuthenticationToken convert(Jwt jwt) {
                UserPrincipal principal = new UserPrincipal(
                        extractUserId(jwt),
                        firstText(jwt, "email", "preferred_username", "sub"),
                        firstText(jwt, AuthConstants.JWT_CLAIM_NICKNAME, "preferred_username", "email", "sub"),
                        resolveRole(jwt)
                );
                return new UsernamePasswordAuthenticationToken(principal, jwt, principal.getAuthorities());
            }
        };
    }

    private Long extractUserId(Jwt jwt) {
        Object claim = jwt.getClaim(AuthConstants.JWT_CLAIM_USER_ID);
        if (claim instanceof Number number) {
            return number.longValue();
        }
        if (claim instanceof String value && StringUtils.hasText(value)) {
            return Long.parseLong(value);
        }
        throw new IllegalStateException("Keycloak access token에 userId claim이 없습니다.");
    }

    private String resolveRole(Jwt jwt) {
        String role = firstText(jwt, AuthConstants.JWT_CLAIM_ROLE);
        if (StringUtils.hasText(role)) {
            return role;
        }

        return extractRealmRoles(jwt).stream()
                .filter(this::isApplicationRole)
                .findFirst()
                .map(this::normalizeRole)
                .orElse("USER");
    }

    private List<String> extractRealmRoles(Jwt jwt) {
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
                .toList();
    }

    private boolean isApplicationRole(String role) {
        return !role.startsWith("default-roles-")
                && !"offline_access".equals(role)
                && !"uma_authorization".equals(role)
                && !"internal_service".equals(role);
    }

    private String normalizeRole(String role) {
        return role.toUpperCase().replace('-', '_');
    }

    private String firstText(Jwt jwt, String... claimNames) {
        for (String claimName : claimNames) {
            String value = jwt.getClaimAsString(claimName);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
