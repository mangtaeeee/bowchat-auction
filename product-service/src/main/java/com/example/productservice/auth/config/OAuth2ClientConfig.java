package com.example.productservice.auth.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(OAuth2InternalClientProperties.class)
public class OAuth2ClientConfig {

    @Bean
    @ConditionalOnProperty(prefix = "oauth2.internal-client", name = {"registration-id", "client-id", "client-secret", "token-uri"})
    public ClientRegistrationRepository clientRegistrationRepository(OAuth2InternalClientProperties properties) {
        ClientRegistration registration = ClientRegistration.withRegistrationId(properties.getRegistrationId())
                .clientId(properties.getClientId())
                .clientSecret(properties.getClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri(properties.getTokenUri())
                .scope(properties.getScopes())
                .build();
        return new InMemoryClientRegistrationRepository(registration);
    }

    @Bean
    @ConditionalOnProperty(prefix = "oauth2.internal-client", name = {"registration-id", "client-id", "client-secret", "token-uri"})
    public OAuth2AuthorizedClientService oAuth2AuthorizedClientService(ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }

    @Bean
    @ConditionalOnProperty(prefix = "oauth2.internal-client", name = {"registration-id", "client-id", "client-secret", "token-uri"})
    public OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService oAuth2AuthorizedClientService
    ) {
        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        clientRegistrationRepository,
                        oAuth2AuthorizedClientService
                );
        manager.setAuthorizedClientProvider(
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .clientCredentials()
                        .build()
        );
        return manager;
    }

    @Bean
    @ConditionalOnProperty(prefix = "oauth2.internal-client", name = "issuer-uri")
    public JwtDecoder jwtDecoder(OAuth2InternalClientProperties properties) {
        return JwtDecoders.fromIssuerLocation(properties.getIssuerUri());
    }

    public static String resolveAccessToken(OAuth2AuthorizedClientManager manager, String registrationId) {
        if (!StringUtils.hasText(registrationId)) {
            return null;
        }

        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
                .withClientRegistrationId(registrationId)
                .principal(new AnonymousAuthenticationToken(
                        "system",
                        "feign-client",
                        AuthorityUtils.createAuthorityList("ROLE_SYSTEM")))
                .build();

        var authorizedClient = manager.authorize(request);
        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            throw new IllegalStateException("Failed to authorize OAuth2 client for registrationId=" + registrationId);
        }

        return authorizedClient.getAccessToken().getTokenValue();
    }
}
