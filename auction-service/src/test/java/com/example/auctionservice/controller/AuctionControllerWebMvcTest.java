package com.example.auctionservice.controller;

import com.example.auctionservice.auth.JwtProvider;
import com.example.auctionservice.auth.UserPrincipal;
import com.example.auctionservice.auth.config.SecurityConfig;
import com.example.auctionservice.auth.filter.InternalServiceAuthenticationFilter;
import com.example.auctionservice.dto.response.AuctionResponse;
import com.example.auctionservice.entity.AuctionErrorCode;
import com.example.auctionservice.entity.AuctionException;
import com.example.auctionservice.exception.GlobalExceptionHandler;
import com.example.auctionservice.service.AuctionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuctionController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, InternalServiceAuthenticationFilter.class})
class AuctionControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuctionService auctionService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void getAuctionReturnsUnauthorizedWhenAuthenticationIsMissing() throws Exception {
        // 인증 정보가 없으면 컨트롤러에 진입하기 전에 401 규약으로 응답해야 한다.
        mockMvc.perform(get("/api/auctions/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    @Test
    void startAuctionReturnsBadRequestWhenPayloadIsInvalid() throws Exception {
        // 요청 본문 검증 실패는 서비스 로직까지 가지 않고 400 규약으로 내려와야 한다.
        String requestBody = """
                {
                  "startingPrice": 0,
                  "endTime": null
                }
                """;

        mockMvc.perform(post("/api/auctions/10/start")
                        .with(authentication(authenticatedUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.context.startingPrice").value("시작 가격은 0보다 커야 합니다."))
                .andExpect(jsonPath("$.context.endTime").value("경매 종료 시간은 필수입니다."));
    }

    @Test
    void startAuctionReturnsForbiddenWhenRequesterIsNotSeller() throws Exception {
        // 서비스가 권한 예외를 던지면 HTTP 계층에서는 403으로 번역돼야 한다.
        doThrow(new AuctionException(AuctionErrorCode.ONLY_SELLER_CAN_START_AUCTION))
                .when(auctionService)
                .startAuction(any(Long.class), any(Long.class), any());

        String requestBody = """
                {
                  "startingPrice": 1000,
                  "endTime": "2099-01-01 10:00:00"
                }
                """;

        mockMvc.perform(post("/api/auctions/10/start")
                        .with(authentication(authenticatedUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ONLY_SELLER_CAN_START_AUCTION"));
    }

    @Test
    void getAuctionReturnsNotFoundWhenAuctionDoesNotExist() throws Exception {
        // 조회 대상이 없을 때는 404와 에러 코드를 그대로 유지해야 한다.
        when(auctionService.findAuctionById(99L))
                .thenThrow(new AuctionException(AuctionErrorCode.AUCTION_NOT_FOUND));

        mockMvc.perform(get("/api/auctions/99")
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AUCTION_NOT_FOUND"));
    }

    @Test
    void getAuctionReturnsAuctionResponseWhenRequestSucceeds() throws Exception {
        // 정상 조회는 상태 코드와 응답 본문 필드가 함께 맞아야 한다.
        when(auctionService.findAuctionById(1L))
                .thenReturn(new AuctionResponse(
                        1L,
                        30L,
                        15_000L,
                        10_000L,
                        LocalDateTime.of(2099, 1, 1, 9, 0),
                        LocalDateTime.of(2099, 1, 1, 10, 0),
                        2L,
                        false
                ));

        mockMvc.perform(get("/api/auctions/1")
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.productId").value(30L))
                .andExpect(jsonPath("$.currentPrice").value(15_000L));
    }

    private Authentication authenticatedUser() {
        UserPrincipal principal = new UserPrincipal(1L, "seller@test.com", "seller", "USER");
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
