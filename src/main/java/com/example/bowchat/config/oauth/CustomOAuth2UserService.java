package com.example.bowchat.config.oauth;

import com.example.bowchat.user.entity.PrincipalDetails;
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

        String provider = request.getClientRegistration().getRegistrationId(); // google, naver
        String email = oAuth2User.getAttribute("email");

        System.out.println("email = " + email);
        System.out.println("provider = " + provider);

        // 기존 사용자 찾기 / 없으면 생성
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
