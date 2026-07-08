package com.example.mapservice.region.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin

/**
 * 좌표를 기준으로 동네를 해석할 때 받는 요청 DTO.
 *
 * Kotlin에서는 Java record 대신 data class를 DTO로 쓰는 편이 일반적이다.
 * 검증 어노테이션이 붙어 있으므로 nullable 타입으로 둘 필요가 없다.
 */
@Schema(description = "좌표 기반 지역 해석 요청")
data class ResolveRegionRequest(
    @field:Schema(description = "위도", example = "37.5665")
    @field:DecimalMin("-90.0")
    @field:DecimalMax("90.0")
    val latitude: Double,
    @field:Schema(description = "경도", example = "126.9780")
    @field:DecimalMin("-180.0")
    @field:DecimalMax("180.0")
    val longitude: Double
)
