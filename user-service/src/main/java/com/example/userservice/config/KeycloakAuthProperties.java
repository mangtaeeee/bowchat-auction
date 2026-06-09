package com.example.userservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@Getter
@Setter
@ConfigurationProperties(prefix = "keycloak.auth")
public class KeycloakAuthProperties {

    private String issuerUri;
    private String tokenUri;
    private String logoutUri;
    private String loginClientId;
    private String loginClientSecret;
    private String adminClientId;
    private String adminClientSecret;

    public String getTokenUri() {
        if (StringUtils.hasText(tokenUri)) {
            return tokenUri;
        }
        return requiredIssuerUri() + "/protocol/openid-connect/token";
    }

    public String getLogoutUri() {
        if (StringUtils.hasText(logoutUri)) {
            return logoutUri;
        }
        return requiredIssuerUri() + "/protocol/openid-connect/logout";
    }

    public String getRealmName() {
        String resolvedIssuerUri = requiredIssuerUri();
        if (!resolvedIssuerUri.contains("/realms/")) {
            throw new IllegalStateException("keycloak.auth.issuer-uri must contain /realms/{realm}");
        }
        return resolvedIssuerUri.substring(resolvedIssuerUri.lastIndexOf('/') + 1);
    }

    public String getAdminUsersUri() {
        String resolvedIssuerUri = requiredIssuerUri();
        int realmIndex = resolvedIssuerUri.indexOf("/realms/");
        if (realmIndex < 0) {
            throw new IllegalStateException("keycloak.auth.issuer-uri must contain /realms/{realm}");
        }
        String baseUri = resolvedIssuerUri.substring(0, realmIndex);
        return baseUri + "/admin/realms/" + getRealmName() + "/users";
    }

    private String requiredIssuerUri() {
        if (!StringUtils.hasText(issuerUri)) {
            throw new IllegalStateException("keycloak.auth.issuer-uri must not be blank");
        }
        return issuerUri;
    }
}
