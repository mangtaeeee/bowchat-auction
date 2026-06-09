package com.example.userservice.auth;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;

public class UserJwtAuthenticationToken extends AbstractAuthenticationToken {

    private final Jwt jwt;
    private final UserPrincipal principal;

    public UserJwtAuthenticationToken(
            Jwt jwt,
            UserPrincipal principal,
            Collection<? extends GrantedAuthority> authorities
    ) {
        super(authorities);
        this.jwt = jwt;
        this.principal = principal;
        setAuthenticated(true);
    }

    public Jwt getJwt() {
        return jwt;
    }

    @Override
    public Object getCredentials() {
        return jwt.getTokenValue();
    }

    @Override
    public UserPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public String getName() {
        return principal.email();
    }
}
