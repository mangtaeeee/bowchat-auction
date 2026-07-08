package com.example.mapservice.region.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

/**
 * 동네 인증 결과 응답 DTO.
 *
 * `resolvedRegionCode`와 `resolvedRegionName`을 함께 내려주는 이유는
 * 상위 서비스가 실패 원인을 바로 판단할 수 있게 하기 위해서다.
 */
@Schema(description = "동네 인증 응답")
data class VerifyRegionResponse(
    @field:Schema(description = "검증 요청 주체의 사용자 ID", example = "1")
    val userId: Long,
    @field:Schema(description = "인증 성공 여부", example = "true")
    val verified: Boolean,
    @field:Schema(description = "현재 좌표로 해석된 지역 코드", example = "4113565500")
    val resolvedRegionCode: String,
    @field:Schema(description = "현재 좌표로 해석된 지역 이름", example = "경기도 성남시 분당구 삼평동")
    val resolvedRegionName: String,
    @field:Schema(description = "인증 시각", example = "2026-07-03T10:00:00Z")
    val verifiedAt: Instant
)
