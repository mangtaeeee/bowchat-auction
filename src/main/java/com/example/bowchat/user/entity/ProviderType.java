package com.example.bowchat.user.entity;

public enum ProviderType {
    LOCAL("local"),
    GOOGLE("google"),
    KAKAO("kakao"),
    NAVER("naver");

    private final String provider;

    ProviderType(String provider) {
        this.provider = provider;
    }

    public String getProvider() {
        return provider;
    }

    public static ProviderType fromString(String provider) {
        for (ProviderType type : ProviderType.values()) {
            if (type.provider.equalsIgnoreCase(provider)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown provider: " + provider);
    }
}
