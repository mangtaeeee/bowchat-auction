package com.example.productservice.auth.config;

import com.example.productservice.auth.JwtProvider;
import com.example.productservice.auth.filter.InternalServiceAuthenticationFilter;
import com.example.productservice.auth.filter.JwtAuthenticationFilter;
import com.example.productservice.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String INTERNAL_SCOPE = "SCOPE_auction.internal.read";

    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final InternalServiceAuthenticationFilter internalServiceAuthenticationFilter;

    @Bean
    @Order(1)
    public SecurityFilterChain internalFilterChain(HttpSecurity http, ObjectProvider<JwtDecoder> jwtDecoderProvider) throws Exception {
        http
                .securityMatcher("/internal/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().hasAnyAuthority(
                                InternalServiceAuthenticationFilter.INTERNAL_SERVICE_ROLE,
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
                )
                .addFilterBefore(internalServiceAuthenticationFilter, BearerTokenAuthenticationFilter.class);

        JwtDecoder jwtDecoder = jwtDecoderProvider.getIfAvailable();
        if (jwtDecoder != null) {
            http.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.decoder(jwtDecoder)));
        }

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, ex) ->
                                writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                                        "UNAUTHORIZED", "인증이 필요합니다."))
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtProvider, redisTemplate),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

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
