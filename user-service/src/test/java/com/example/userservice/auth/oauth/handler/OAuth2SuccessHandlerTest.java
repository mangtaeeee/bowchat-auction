package com.example.userservice.auth.oauth.handler;

import com.example.userservice.auth.service.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    @Mock
    private OAuth2AuthorizedClientService authorizedClientService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Test
    void onAuthenticationSuccessRedirectsWithoutAccessTokenInQuery() throws Exception {
        OAuth2SuccessHandler handler = new OAuth2SuccessHandler(authorizedClientService, refreshTokenService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "Mozilla/5.0");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Keycloak 로그인 성공 직후 Spring Security가 넘겨주는 OIDC 사용자 객체를 흉내 낸다.
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority("USER")),
                new OidcIdToken(
                        "id-token",
                        Instant.now(),
                        Instant.now().plusSeconds(300),
                        Map.of(
                                "sub", "sub-123",
                                "email", "user@test.com"
                        )
                )
        );
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                oidcUser,
                oidcUser.getAuthorities(),
                "keycloak"
        );

        // AuthorizedClient 안에는 실제로 발급된 access/refresh token이 들어있다.
        // 이 테스트는 "access token을 URL에 붙이지 않고 refresh token만 저장한 뒤 리다이렉트하는지"를 본다.
        OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(
                ClientRegistration.withRegistrationId("keycloak")
                        .clientId("bowchat-web")
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                        .authorizationUri("https://issuer.example/auth")
                        .tokenUri("https://issuer.example/token")
                        .jwkSetUri("https://issuer.example/jwks")
                        .issuerUri("https://issuer.example")
                        .userInfoUri("https://issuer.example/userinfo")
                        .userNameAttributeName("sub")
                        .clientName("keycloak")
                        .build(),
                authentication.getName(),
                new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        "access-token",
                        Instant.now(),
                        Instant.now().plusSeconds(300)
                ),
                new OAuth2RefreshToken(
                        "refresh-token",
                        Instant.now(),
                        Instant.now().plusSeconds(600)
                )
        );

        when(authorizedClientService.loadAuthorizedClient("keycloak", authentication.getName()))
                .thenReturn(authorizedClient);

        handler.onAuthenticationSuccess(request, response, authentication);

        // access token이 query string으로 새지 않도록 고정 경로로만 이동한다.
        assertThat(response.getRedirectedUrl()).isEqualTo("/view/product");
        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        verify(refreshTokenService).save(
                org.mockito.ArgumentMatchers.eq("user@test.com"),
                org.mockito.ArgumentMatchers.eq("refresh-token"),
                ttlCaptor.capture()
        );
        // 만료 시간은 테스트 실행 시점에 따라 조금씩 달라질 수 있으므로 범위로 검증한다.
        assertThat(ttlCaptor.getValue()).isPositive().isLessThanOrEqualTo(600_000L);
    }
}
