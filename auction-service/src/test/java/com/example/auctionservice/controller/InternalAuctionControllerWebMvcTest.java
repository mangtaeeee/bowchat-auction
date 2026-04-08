package com.example.auctionservice.controller;

import com.example.auctionservice.auth.JwtProvider;
import com.example.auctionservice.auth.config.SecurityConfig;
import com.example.auctionservice.auth.filter.InternalServiceAuthenticationFilter;
import com.example.auctionservice.dto.response.AuctionResponse;
import com.example.auctionservice.exception.GlobalExceptionHandler;
import com.example.auctionservice.service.AuctionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InternalAuctionController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, InternalServiceAuthenticationFilter.class})
class InternalAuctionControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuctionService auctionService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void getAuctionReturnsUnauthorizedWhenInternalCredentialsAreMissing() throws Exception {
        mockMvc.perform(get("/internal/auctions/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED_INTERNAL_API"));
    }

    @Test
    void getAuctionAllowsLegacyInternalServiceToken() throws Exception {
        when(auctionService.findAuctionById(1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/internal/auctions/1")
                        .header(InternalServiceAuthenticationFilter.INTERNAL_TOKEN_HEADER, "test-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getAuctionAllowsOAuth2ClientCredentialsToken() throws Exception {
        when(auctionService.findAuctionById(1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/internal/auctions/1")
                        .with(jwt().authorities(() -> "SCOPE_auction.internal.read")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.productId").value(10L));
    }

    @Test
    void getAuctionRejectsOAuth2TokenWithoutRequiredScope() throws Exception {
        when(auctionService.findAuctionById(1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/internal/auctions/1")
                        .with(jwt().authorities(() -> "SCOPE_user.read")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_INTERNAL_API"));
    }

    private AuctionResponse sampleResponse() {
        return new AuctionResponse(
                1L,
                10L,
                20_000L,
                10_000L,
                LocalDateTime.of(2099, 1, 1, 9, 0),
                LocalDateTime.of(2099, 1, 1, 10, 0),
                3L,
                false
        );
    }
}

