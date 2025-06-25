package com.example.bowchat.config.oauth.service;

import com.example.bowchat.config.oauth.userinfo.OAuth2UserInfo;
import com.example.bowchat.config.oauth.util.OAuth2UserInfoFactory;
import com.example.bowchat.user.entity.PrincipalDetails;
import com.example.bowchat.user.entity.ProviderType;
import com.example.bowchat.user.entity.Role;
import com.example.bowchat.user.entity.User;
import com.example.bowchat.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        log.info("1. OAuth2User 가져오기 ");
        OAuth2User oAuth2User = super.loadUser(request);

        log.info("2. OAuth2User 정보 ");
        String providerName = request.getClientRegistration().getRegistrationId();
        ProviderType provider = getProviderType(providerName);

        log.info("3.  Provider 별로 attribute 매핑");
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(provider, oAuth2User.getAttributes());
        String email = oAuth2UserInfo.getEmail();
        if (email == null || email.isEmpty()) {
            log.error("OAuth2 로그인 실패: 이메일이 없습니다. provider={}", provider);
            throw new OAuth2AuthenticationException("이메일을 제공하지 않는 SNS입니다.");
        }

        log.info("4. 사용자 정보 조회 또는 신규 사용자 등록");
        User user = userRepository.findByEmailAndProvider(email,provider)
                .map(existingUser -> updateExistingUser(existingUser, oAuth2UserInfo))
                .orElseGet(() -> registerNewUser(provider, oAuth2UserInfo));

        log.info("5. PrincipalDetails 생성");
        return new PrincipalDetails(user, oAuth2User.getAttributes());
    }

    private User registerNewUser(ProviderType provider, OAuth2UserInfo oAuth2UserInfo) {
        User newUser = User.builder()
                .email(oAuth2UserInfo.getEmail())
                .provider(provider)
                .providerId(oAuth2UserInfo.getProviderId())
                .nickname(generateNickname(oAuth2UserInfo))
                .role(Role.USER)
                .build();
        log.info("신규 사용자 등록: email={}, provider={}", newUser.getEmail(), provider);
        return userRepository.save(newUser);
    }


    private ProviderType getProviderType(String providerName) {
        try {
            return ProviderType.valueOf(providerName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new OAuth2AuthenticationException("지원하지 않는 로그인 제공자입니다: " + providerName);
        }
    }

    private User updateExistingUser(User user, OAuth2UserInfo userInfo) {
        boolean updated = false;
        // 예: 닉네임이나 프로필 사진을 SNS 최신 정보로 업데이트
        String newNickname = generateNickname(userInfo);
        if (!newNickname.equals(user.getNickname())) {
            user.updateNickname(newNickname);
            updated = true;
        }
        // 추가 필드 비교 후 업데이트 가능
        if (updated) {
            log.info("기존 사용자 정보 업데이트: email={}", user.getEmail());
            userRepository.save(user);
        }
        return user;
    }

    private String generateNickname(OAuth2UserInfo userInfo) {
        // 이메일 앞부분 대신, provider가 줄 수 있는 이름(username, name 등)을 우선 활용
        return userInfo.getName() != null && !userInfo.getName().isEmpty()
                ? userInfo.getName()
                : userInfo.getEmail().split("@")[0];
    }
}
