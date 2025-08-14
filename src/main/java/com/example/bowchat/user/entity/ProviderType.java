package com.example.bowchat.user.entity;

import com.example.bowchat.user.auth.oauth.userinfo.GoogleOAuth2UserInfo;
import com.example.bowchat.user.auth.oauth.userinfo.KakaoOAuth2UserInfo;
import com.example.bowchat.user.auth.oauth.userinfo.NaverOAuth2UserInfo;
import com.example.bowchat.user.auth.oauth.userinfo.OAuth2UserInfo;
import lombok.Getter;

import java.util.Map;
import java.util.function.Function;

@Getter
public enum ProviderType {
    LOCAL(attributes -> {
        throw new UnsupportedOperationException("Local provider does not support OAuth2UserInfo creation");
    }),
    GOOGLE(GoogleOAuth2UserInfo::new),
    KAKAO(KakaoOAuth2UserInfo::new),
    NAVER(NaverOAuth2UserInfo::new);

    private final Function<Map<String, Object>, OAuth2UserInfo> creator;

    ProviderType(Function<Map<String, Object>, OAuth2UserInfo> creator) {
        this.creator = creator;
    }

    public OAuth2UserInfo create(Map<String, Object> attributes) {
        return creator.apply(attributes);
    }
}
