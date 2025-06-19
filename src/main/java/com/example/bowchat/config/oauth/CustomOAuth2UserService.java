package com.example.bowchat.config.oauth;

import com.example.bowchat.user.entity.PrincipalDetails;
import com.example.bowchat.user.entity.ProviderType;
import com.example.bowchat.user.entity.Role;
import com.example.bowchat.user.entity.User;
import com.example.bowchat.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(request);

        String providerName = request.getClientRegistration().getRegistrationId();
        String email = oAuth2User.getAttribute("email");

        if (email == null || email.isEmpty()) {
            throw new OAuth2AuthenticationException("이메일을 제공하지 않는 SNS입니다.");
        }

        ProviderType provider;
        try {
            provider = ProviderType.valueOf(providerName.toUpperCase()); // ex) "google" -> GOOGLE
        } catch (IllegalArgumentException e) {
            throw new OAuth2AuthenticationException("지원하지 않는 로그인 제공자입니다: " + providerName);
        }

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .provider(provider)
                            .nickname(Objects.requireNonNull(email).split("@")[0])
                            .role(Role.USER)
                            .build();
                    return userRepository.save(newUser);
                });

        return new PrincipalDetails(user, oAuth2User.getAttributes());
    }
}
