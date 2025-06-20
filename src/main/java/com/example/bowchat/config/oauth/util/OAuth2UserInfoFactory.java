// OAuth2UserInfoFactory.java
package com.example.bowchat.config.oauth.util;

import com.example.bowchat.config.oauth.userinfo.OAuth2UserInfo;
import com.example.bowchat.user.entity.ProviderType;

import java.util.Map;

public class OAuth2UserInfoFactory {
    public static OAuth2UserInfo getOAuth2UserInfo(ProviderType provider, Map<String, Object> attributes) {
        return provider.create(attributes);
    }
}