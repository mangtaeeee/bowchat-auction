// OAuth2UserInfoFactory.java
package com.example.bowchat.user.auth.oauth.util;

import com.example.bowchat.user.auth.oauth.userinfo.OAuth2UserInfo;
import com.example.bowchat.user.entity.ProviderType;

import java.util.Map;

public class OAuth2UserInfoFactory {
    public static OAuth2UserInfo getOAuth2UserInfo(ProviderType provider, Map<String, Object> attributes) {
        return provider.create(attributes);
    }
}