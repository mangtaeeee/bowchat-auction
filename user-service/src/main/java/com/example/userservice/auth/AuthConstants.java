package com.example.userservice.auth;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuthConstants {

    public static final String INTERNAL_PATH_PREFIX = "/internal/";
    public static final String INTERNAL_PATH_PATTERN = "/internal/**";
    public static final String INTERNAL_SERVICE_ROLE = "ROLE_INTERNAL_SERVICE";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final int BEARER_PREFIX_LENGTH = BEARER_PREFIX.length();
    public static final String JWT_CLAIM_USER_ID = "userId";
    public static final String JWT_CLAIM_NICKNAME = "nickname";
    public static final String JWT_CLAIM_ROLE = "role";
    public static final String BLACKLIST_PREFIX = "blacklist:";
    public static final String REFRESH_TOKEN_REDIS_PREFIX = "refresh_token:";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    public static final String LOGOUT_MARKER = "logout";
}
