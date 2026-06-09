package com.example.userservice.auth.oauth.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.LinkedHashMap;
import java.util.Map;

public class KeycloakAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private static final String AUTHORIZATION_BASE_URI = "/oauth2/authorization";
    private static final String KEYCLOAK_REGISTRATION_ID = "keycloak";

    private final DefaultOAuth2AuthorizationRequestResolver defaultResolver;

    public KeycloakAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository,
                AUTHORIZATION_BASE_URI
        );
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        String provider = resolveProvider(request);
        if (provider == null) {
            return defaultResolver.resolve(request);
        }
        return customize(defaultResolver.resolve(request, KEYCLOAK_REGISTRATION_ID), provider);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        String provider = resolveProvider(request);
        String registrationId = provider == null ? clientRegistrationId : KEYCLOAK_REGISTRATION_ID;
        return customize(defaultResolver.resolve(request, registrationId), provider);
    }

    private OAuth2AuthorizationRequest customize(OAuth2AuthorizationRequest request, String provider) {
        if (request == null || provider == null) {
            return request;
        }

        Map<String, Object> additionalParameters = new LinkedHashMap<>(request.getAdditionalParameters());
        additionalParameters.put("kc_idp_hint", provider);
        return OAuth2AuthorizationRequest.from(request)
                .additionalParameters(additionalParameters)
                .build();
    }

    private String resolveProvider(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null || !path.startsWith(AUTHORIZATION_BASE_URI + "/")) {
            return null;
        }

        String registrationId = path.substring((AUTHORIZATION_BASE_URI + "/").length()).toLowerCase();
        return switch (registrationId) {
            case "google", "kakao" -> registrationId;
            default -> null;
        };
    }
}
