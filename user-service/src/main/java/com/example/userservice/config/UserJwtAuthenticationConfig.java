package com.example.userservice.config;

import com.example.userservice.auth.AuthConstants;
import com.example.userservice.auth.UserJwtAuthenticationToken;
import com.example.userservice.auth.UserPrincipal;
import com.example.userservice.entity.Role;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import java.util.List;

@Configuration
public class UserJwtAuthenticationConfig {

    @Bean
    @Qualifier("userJwtDecoder")
    @ConditionalOnProperty(prefix = "keycloak.auth", name = "issuer-uri")
    public JwtDecoder userJwtDecoder(KeycloakAuthProperties properties) {
        return JwtDecoders.fromIssuerLocation(properties.getIssuerUri());
    }

    @Bean
    @Qualifier("userJwtAuthenticationConverter")
    @ConditionalOnProperty(prefix = "keycloak.auth", name = "issuer-uri")
    public Converter<Jwt, ? extends AbstractAuthenticationToken> userJwtAuthenticationConverter() {
        return new UserJwtAuthenticationConverter();
    }

    private final class UserJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
        @Override
        public AbstractAuthenticationToken convert(Jwt jwt) {
            Long userId = requiredLongClaim(jwt, AuthConstants.JWT_CLAIM_USER_ID);
            String email = requiredTextClaim(jwt, "email");
            String nickname = requiredTextClaim(jwt, AuthConstants.JWT_CLAIM_NICKNAME);
            Role role = requiredRole(jwt);
            return new UserJwtAuthenticationToken(
                    jwt,
                    new UserPrincipal(
                            userId,
                            email,
                            nickname,
                            role
                    ),
                    List.of(new SimpleGrantedAuthority(role.getAuthority()))
            );
        }
    }

    private Long requiredLongClaim(Jwt jwt, String claimName) {
        Object value = jwt.getClaims().get(claimName);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ex) {
                throw invalidToken(claimName + " claim must be numeric", ex);
            }
        }
        throw invalidToken(claimName + " claim is required");
    }

    private String requiredTextClaim(Jwt jwt, String claimName) {
        String value = jwt.getClaimAsString(claimName);
        if (value == null || value.isBlank()) {
            throw invalidToken(claimName + " claim is required");
        }
        return value;
    }

    private Role requiredRole(Jwt jwt) {
        String roleClaim = requiredTextClaim(jwt, AuthConstants.JWT_CLAIM_ROLE);
        try {
            return Role.valueOf(roleClaim);
        } catch (IllegalArgumentException ex) {
            throw invalidToken("role claim is invalid", ex);
        }
    }

    private InvalidBearerTokenException invalidToken(String message) {
        return new InvalidBearerTokenException(message);
    }

    private InvalidBearerTokenException invalidToken(String message, Throwable cause) {
        return new InvalidBearerTokenException(message, cause);
    }
}
