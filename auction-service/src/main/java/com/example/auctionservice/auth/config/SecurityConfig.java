package com.example.auctionservice.auth.config;

import com.example.auctionservice.auth.AuthConstants;
import com.example.auctionservice.auth.filter.AccessTokenBlacklistFilter;
import com.example.auctionservice.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(CorsProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String INTERNAL_SCOPE = "SCOPE_user.internal.read";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final CorsProperties corsProperties;

    @Bean
    @Order(1)
    public SecurityFilterChain internalFilterChain(HttpSecurity http, ObjectProvider<JwtDecoder> jwtDecoderProvider) throws Exception {
        http
                .securityMatcher(AuthConstants.INTERNAL_PATH_PATTERN)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().hasAnyAuthority(
                                AuthConstants.INTERNAL_SERVICE_ROLE,
                                INTERNAL_SCOPE
                        )
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, ex) ->
                                writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                                        "UNAUTHORIZED_INTERNAL_API", "내부 API 인증이 필요합니다."))
                        .accessDeniedHandler((request, response, ex) ->
                                writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN,
                                        "FORBIDDEN_INTERNAL_API", "내부 API 접근 권한이 없습니다."))
                );

        JwtDecoder jwtDecoder = jwtDecoderProvider.getIfAvailable();
        if (jwtDecoder != null) {
            http.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.decoder(jwtDecoder)));
        }

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiFilterChain(
            HttpSecurity http,
            ObjectProvider<JwtDecoder> jwtDecoderProvider,
            ObjectProvider<Converter<Jwt, ? extends AbstractAuthenticationToken>> userJwtAuthenticationConverterProvider
    ) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/assets/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auctions", "/api/auctions/*").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, ex) ->
                                writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                                        "UNAUTHORIZED", "인증이 필요합니다."))
                )
                .addFilterBefore(
                        new AccessTokenBlacklistFilter(redisTemplate, objectMapper),
                        UsernamePasswordAuthenticationFilter.class);

        JwtDecoder jwtDecoder = jwtDecoderProvider.getIfAvailable();
        if (jwtDecoder != null) {
            Converter<Jwt, ? extends AbstractAuthenticationToken> converter =
                    userJwtAuthenticationConverterProvider.getIfAvailable();
            http.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> {
                jwt.decoder(jwtDecoder);
                if (converter != null) {
                    jwt.jwtAuthenticationConverter(converter);
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

    private void writeErrorResponse(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(code, message));
    }
}

