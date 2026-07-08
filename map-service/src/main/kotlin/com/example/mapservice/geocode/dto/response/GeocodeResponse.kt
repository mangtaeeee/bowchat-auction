package com.example.mapservice.geocode.dto.response

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 주소를 좌표로 변환한 결과.
 */
@Schema(description = "주소를 좌표로 변환한 응답")
data class GeocodeResponse(
    @field:Schema(description = "입력으로 받은 주소 원문", example = "경기 성남시 분당구 판교역로 166")
    val query: String,
    @field:Schema(description = "정규화된 주소 기준 위도", example = "37.402697")
    val latitude: Double,
    @field:Schema(description = "정규화된 주소 기준 경도", example = "127.104599")
    val longitude: Double,
    @field:Schema(description = "카카오 응답 기준으로 정규화된 주소", example = "경기 성남시 분당구 판교역로 166")
    val normalizedAddress: String
)
