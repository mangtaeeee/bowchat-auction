package com.example.userservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationYamlOAuth2ScopeTest {

    @Test
    void keycloakBrowserClientRequestsCustomUserClaimsScope() {
        // application.yaml을 직접 읽어서
        // 브라우저 OAuth client가 Keycloak custom scope를 요청하도록 고정돼 있는지 확인한다.
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application.yaml"));
        Properties properties = factory.getObject();

        // openid/profile/email만 있으면 userId/nickname/role mapper가 토큰에 안 실릴 수 있다.
        // 그래서 bowchat.user.claims가 scope 목록에 반드시 들어가야 한다.
        assertThat(properties).isNotNull();
        assertThat(properties.getProperty("spring.security.oauth2.client.registration.keycloak.scope[0]"))
                .isEqualTo("openid");
        assertThat(properties.getProperty("spring.security.oauth2.client.registration.keycloak.scope[1]"))
                .isEqualTo("profile");
        assertThat(properties.getProperty("spring.security.oauth2.client.registration.keycloak.scope[2]"))
                .isEqualTo("email");
        assertThat(properties.getProperty("spring.security.oauth2.client.registration.keycloak.scope[3]"))
                .isEqualTo("bowchat.user.claims");
    }
}
