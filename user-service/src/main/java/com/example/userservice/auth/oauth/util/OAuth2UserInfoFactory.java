// OAuth2UserInfoFactory.java
package com.example.userservice.auth.oauth.util;


import com.example.userservice.auth.oauth.userinfo.OAuth2UserInfo;
import com.example.userservice.entity.ProviderType;

import java.util.Map;

public class OAuth2UserInfoFactory {
    public static OAuth2UserInfo getOAuth2UserInfo(ProviderType provider, Map<String, Object> attributes) {
        return provider.create(attributes);
    }
}