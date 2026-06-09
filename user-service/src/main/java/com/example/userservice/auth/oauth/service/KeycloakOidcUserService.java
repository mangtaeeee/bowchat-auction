package com.example.userservice.auth.oauth.service;

import com.example.userservice.entity.ProviderType;
import com.example.userservice.entity.Role;
import com.example.userservice.entity.User;
import com.example.userservice.event.OutboxEventPublisher;
import com.example.userservice.event.UserCreatedEvent;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.auth.service.KeycloakAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakOidcUserService extends OidcUserService {

    private final UserRepository userRepository;
    private final OutboxEventPublisher outboxEventPublisher;
    private final KeycloakAuthService keycloakAuthService;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        String email = oidcUser.getEmail();
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Keycloak 브로커 로그인 사용자 이메일이 없습니다.");
        }

        User user = userRepository.findByEmailAndProvider(email, ProviderType.KEYCLOAK)
                .map(existingUser -> updateExistingUser(existingUser, oidcUser))
                .orElseGet(() -> registerNewUser(oidcUser, email));

        keycloakAuthService.syncUserProfile(user);
        return oidcUser;
    }

    private User registerNewUser(OidcUser oidcUser, String email) {
        User user = User.builder()
                .email(email)
                .nickname(resolveNickname(oidcUser))
                .provider(ProviderType.KEYCLOAK)
                .providerId(oidcUser.getSubject())
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(user);
        outboxEventPublisher.saveUserCreatedEvent(UserCreatedEvent.of(savedUser));
        log.info("Keycloak 브로커 신규 사용자 등록: email={}", email);
        return savedUser;
    }

    private User updateExistingUser(User user, OidcUser oidcUser) {
        String nickname = resolveNickname(oidcUser);
        if (!nickname.equals(user.getNickname())) {
            user.updateNickname(nickname);
            userRepository.save(user);
            log.info("Keycloak 브로커 사용자 닉네임 동기화: email={}", user.getEmail());
        }
        return user;
    }

    private String resolveNickname(OidcUser oidcUser) {
        String preferredUsername = oidcUser.getPreferredUsername();
        if (preferredUsername != null && !preferredUsername.isBlank()) {
            return preferredUsername;
        }

        String fullName = oidcUser.getFullName();
        if (fullName != null && !fullName.isBlank()) {
            return fullName;
        }

        String name = oidcUser.getGivenName();
        if (name != null && !name.isBlank()) {
            return name;
        }

        return oidcUser.getEmail().split("@")[0];
    }
}
