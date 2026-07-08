package com.example.mapservice.region.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin

/**
 * 좌표를 기준으로 행정동 정보를 해석할 때 받는 요청 DTO.
 */
@Schema(description = "좌표 기반 지역 해석 요청")
data class ResolveRegionRequest(
    @field:Schema(description = "해석할 위치의 위도", example = "37.402697")
    @field:DecimalMin("-90.0")
    @field:DecimalMax("90.0")
    val latitude: Double,
    @field:Schema(description = "해석할 위치의 경도", example = "127.104599")
    @field:DecimalMin("-180.0")
    @field:DecimalMax("180.0")
    val longitude: Double
)
