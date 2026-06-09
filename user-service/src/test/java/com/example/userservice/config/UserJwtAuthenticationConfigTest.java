package com.example.userservice.config;

import com.example.userservice.auth.UserJwtAuthenticationToken;
import com.example.userservice.auth.UserPrincipal;
import com.example.userservice.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserJwtAuthenticationConfigTest {

    private final UserJwtAuthenticationConfig config = new UserJwtAuthenticationConfig();

    @Test
    void userJwtAuthenticationConverterBuildsStrictUserPrincipal() {
        // 이 converter는 "일반 사용자 토큰" 전용이다.
        // 필수 클레임이 모두 있으면 도메인에서 쓰기 쉬운 UserPrincipal로 바꿔야 한다.
        Converter<Jwt, ? extends AbstractAuthenticationToken> converter =
                config.userJwtAuthenticationConverter();

        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "RS256"),
                Map.of(
                        "sub", "subject",
                        "email", "user@test.com",
                        "userId", "123",
                        "nickname", "tester",
                        "role", "USER"
                )
        );

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        // JWT 원문만 통과시키는 대신, userId/email/nickname/role가 정리된 principal이 만들어지는지 본다.
        assertThat(authentication).isInstanceOf(UserJwtAuthenticationToken.class);
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("USER");
        assertThat(authentication.getPrincipal()).isEqualTo(
                new UserPrincipal(123L, "user@test.com", "tester", Role.USER)
        );
    }

    @Test
    void userJwtAuthenticationConverterRejectsServiceTokenWithoutUserClaims() {
        // 내부 서비스 토큰(client_credentials)은 일반 사용자 클레임이 없다.
        // 이런 토큰이 일반 API 체인으로 들어오면 인증 자체를 실패시켜야 한다.
        Converter<Jwt, ? extends AbstractAuthenticationToken> converter =
                config.userJwtAuthenticationConverter();

        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "RS256"),
                Map.of(
                        "preferred_username", "auction-service",
                        "scope", "user.internal.read"
                )
        );

        assertThatThrownBy(() -> converter.convert(jwt))
                .isInstanceOf(InvalidBearerTokenException.class)
                .hasMessageContaining("userId claim is required");
    }
}
