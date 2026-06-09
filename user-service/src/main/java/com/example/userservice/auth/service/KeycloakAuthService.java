package com.example.userservice.auth.service;

import com.example.userservice.auth.dto.KeycloakTokenResponse;
import com.example.userservice.auth.dto.KeycloakUserRepresentation;
import com.example.userservice.config.KeycloakAuthProperties;
import com.example.userservice.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KeycloakAuthService {

    private static final ParameterizedTypeReference<List<KeycloakUserRepresentation>> USER_LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final KeycloakAuthProperties properties;

    public KeycloakTokenResponse login(String username, String password) {
        return requestToken(formData(Map.of(
                "grant_type", "password",
                "client_id", properties.getLoginClientId(),
                "client_secret", properties.getLoginClientSecret(),
                "username", username,
                "password", password
        )));
    }

    public KeycloakTokenResponse refresh(String refreshToken) {
        return requestToken(formData(Map.of(
                "grant_type", "refresh_token",
                "client_id", properties.getLoginClientId(),
                "client_secret", properties.getLoginClientSecret(),
                "refresh_token", refreshToken
        )));
    }

    public void logout(String refreshToken) {
        MultiValueMap<String, String> body = formData(Map.of(
                "client_id", properties.getLoginClientId(),
                "client_secret", properties.getLoginClientSecret(),
                "refresh_token", refreshToken
        ));

        restClient.post()
                .uri(properties.getLogoutUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    public void syncLocalUser(User user, String rawPassword) {
        // user-service DB가 도메인 프로필을, Keycloak이 인증 원장을 맡도록 동기화한다.
        String adminToken = obtainAdminAccessToken();
        KeycloakUserRepresentation existingUser = findUserByUsername(adminToken, user.getEmail());
        if (existingUser == null) {
            createUser(adminToken, user);
            existingUser = findUserByUsername(adminToken, user.getEmail());
            if (existingUser == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Keycloak 사용자 생성 확인 실패");
            }
        }

        updateUserProfile(adminToken, existingUser.id(), user);
        updateUserPassword(adminToken, existingUser.id(), rawPassword);
    }

    public void syncUserProfile(User user) {
        String adminToken = obtainAdminAccessToken();
        KeycloakUserRepresentation existingUser = findUserByUsername(adminToken, user.getEmail());
        if (existingUser == null) {
            createUser(adminToken, user);
            return;
        }

        updateUserProfile(adminToken, existingUser.id(), user);
    }

    private KeycloakTokenResponse requestToken(MultiValueMap<String, String> body) {
        try {
            return restClient.post()
                    .uri(properties.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(KeycloakTokenResponse.class);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Keycloak 인증에 실패했습니다.", ex);
        }
    }

    private String obtainAdminAccessToken() {
        KeycloakTokenResponse response = requestToken(formData(Map.of(
                "grant_type", "client_credentials",
                "client_id", properties.getAdminClientId(),
                "client_secret", properties.getAdminClientSecret()
        )));
        return response.accessToken();
    }

    private KeycloakUserRepresentation findUserByUsername(String adminToken, String username) {
        List<KeycloakUserRepresentation> users = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(properties.getAdminUsersUri())
                        .queryParam("username", username)
                        .queryParam("exact", true)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .body(USER_LIST_TYPE);

        if (users == null || users.isEmpty()) {
            return null;
        }
        return users.get(0);
    }

    private void createUser(String adminToken, User user) {
        restClient.post()
                .uri(properties.getAdminUsersUri())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(userRepresentation(user))
                .retrieve()
                .toBodilessEntity();
    }

    private void updateUserProfile(String adminToken, String keycloakUserId, User user) {
        restClient.put()
                .uri(properties.getAdminUsersUri() + "/" + keycloakUserId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(userRepresentation(user))
                .retrieve()
                .toBodilessEntity();
    }

    private void updateUserPassword(String adminToken, String userId, String rawPassword) {
        Map<String, Object> body = Map.of(
                "type", "password",
                "temporary", false,
                "value", rawPassword
        );

        restClient.put()
                .uri(properties.getAdminUsersUri() + "/" + userId + "/reset-password")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private MultiValueMap<String, String> formData(Map<String, String> values) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        values.forEach(body::add);
        return body;
    }

    private Map<String, Object> userRepresentation(User user) {
        return Map.of(
                "username", user.getEmail(),
                "email", user.getEmail(),
                "enabled", true,
                "emailVerified", true,
                "attributes", Map.of(
                        "userId", List.of(String.valueOf(user.getId())),
                        "nickname", List.of(user.getNickname()),
                        "role", List.of(user.getRole().name())
                )
        );
    }
}
