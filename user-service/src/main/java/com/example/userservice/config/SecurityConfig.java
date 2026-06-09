package com.example.userservice.config;

import com.example.userservice.auth.AuthConstants;
import com.example.userservice.auth.CustomAuthenticationProvider;
import com.example.userservice.auth.UserJwtAuthenticationToken;
import com.example.userservice.auth.filter.AccessTokenBlacklistFilter;
import com.example.userservice.auth.oauth.config.KeycloakAuthorizationRequestResolver;
import com.example.userservice.auth.oauth.handler.OAuth2SuccessHandler;
import com.example.userservice.auth.oauth.service.KeycloakOidcUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({CorsProperties.class, AuthFeatureProperties.class})
public class SecurityConfig {

    private static final String INTERNAL_SCOPE = "SCOPE_user.internal.read";

    private final KeycloakOidcUserService keycloakOidcUserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final CorsProperties corsProperties;

    @Bean
    @Order(1)
    public SecurityFilterChain internalFilterChain(
            HttpSecurity http,
            @Qualifier("internalJwtDecoder") ObjectProvider<JwtDecoder> jwtDecoderProvider,
            @Qualifier("internalJwtAuthenticationConverter")
            ObjectProvider<Converter<Jwt, ? extends AbstractAuthenticationToken>> jwtAuthenticationConverterProvider
    ) throws Exception {
        http
                .securityMatcher(AuthConstants.INTERNAL_PATH_PATTERN)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().hasAnyAuthority(
                                AuthConstants.INTERNAL_SERVICE_ROLE,
                                INTERNAL_SCOPE
                        )
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, ex) ->
                                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                                        Map.of("message", "Unauthorized internal API")))
                        .accessDeniedHandler((request, response, ex) ->
                                writeJson(response, HttpServletResponse.SC_FORBIDDEN,
                                        Map.of("message", "Forbidden internal API")))
                );

        JwtDecoder jwtDecoder = jwtDecoderProvider.getIfAvailable();
        if (jwtDecoder != null) {
            Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter =
                    jwtAuthenticationConverterProvider.getIfAvailable();
            http.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> {
                jwt.decoder(jwtDecoder);
                if (jwtAuthenticationConverter != null) {
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter);
                }
            }));
        }

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Qualifier("userJwtDecoder") ObjectProvider<JwtDecoder> jwtDecoderProvider,
            @Qualifier("userJwtAuthenticationConverter")
            ObjectProvider<Converter<Jwt, ? extends AbstractAuthenticationToken>> jwtAuthenticationConverterProvider,
            ObjectProvider<OAuth2AuthorizationRequestResolver> authorizationRequestResolverProvider
    ) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/**",
                                "/user/signup",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/actuator/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint((request, response, authException) ->
                                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                                        Map.of("message", "Unauthorized")))
                )
                .oauth2Login(oauth2 -> {
                    OAuth2AuthorizationRequestResolver authorizationRequestResolver =
                            authorizationRequestResolverProvider.getIfAvailable();
                    if (authorizationRequestResolver != null) {
                        oauth2.authorizationEndpoint(authorization ->
                                authorization.authorizationRequestResolver(authorizationRequestResolver));
                    }
                    oauth2.userInfoEndpoint(userInfo ->
                            userInfo.oidcUserService(keycloakOidcUserService));
                    oauth2.successHandler(oAuth2SuccessHandler);
                })
                .addFilterBefore(
                        new AccessTokenBlacklistFilter(redisTemplate, objectMapper),
                        UsernamePasswordAuthenticationFilter.class);

        JwtDecoder jwtDecoder = jwtDecoderProvider.getIfAvailable();
        if (jwtDecoder != null) {
            Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter =
                    jwtAuthenticationConverterProvider.getIfAvailable();
            http.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> {
                jwt.decoder(jwtDecoder);
                if (jwtAuthenticationConverter != null) {
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter);
                }
            }));
        }

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsProperties.allowedOrigins());
        config.setAllowedMethods(corsProperties.allowedMethods());
        config.setAllowedHeaders(corsProperties.allowedHeaders());
        config.setAllowCredentials(corsProperties.allowCredentials());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http, CustomAuthenticationProvider customAuthenticationProvider) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .authenticationProvider(customAuthenticationProvider)
                .build();
    }

    @Bean
    public OAuth2AuthorizationRequestResolver authorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository
    ) {
        return new KeycloakAuthorizationRequestResolver(clientRegistrationRepository);
    }

    private void writeJson(HttpServletResponse response, int status, Map<String, String> body) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}

