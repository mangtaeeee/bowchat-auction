package com.example.bowchat.user.auth.oauth.userinfo;

import java.util.Map;

public abstract class OAuth2UserInfo {

    protected Map<String ,Object> attributes;
    protected OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
    public abstract String getProviderId();
    public abstract String getEmail();
    public abstract String getName();
    public abstract String getImageUrl();
    public abstract String getProviderType();

}
