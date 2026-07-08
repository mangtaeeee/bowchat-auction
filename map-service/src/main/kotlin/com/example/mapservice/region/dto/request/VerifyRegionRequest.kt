package com.example.mapservice.region.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank

/**
 * 사용자의 현재 좌표가 기대한 동네 코드와 일치하는지 확인하는 요청 DTO.
 *
 * `userId`는 map-service가 유저를 소유한다는 뜻이 아니라,
 * 검증 요청의 주체를 상위 서비스가 추적하기 쉽게 하려는 상관관계 값이다.
 */
@Schema(description = "동네 인증 요청")
data class VerifyRegionRequest(
    @field:Schema(description = "검증 요청 주체의 사용자 ID", example = "1")
    val userId: Long,
    @field:Schema(description = "현재 위치의 위도", example = "37.5665")
    @field:DecimalMin("-90.0")
    @field:DecimalMax("90.0")
    val latitude: Double,
    @field:Schema(description = "현재 위치의 경도", example = "126.9780")
    @field:DecimalMin("-180.0")
    @field:DecimalMax("180.0")
    val longitude: Double,
    @field:Schema(description = "기대하는 지역 코드", example = "1168054500")
    @field:NotBlank
    val expectedRegionCode: String
)
