package com.example.mapservice.geocode.dto.response

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 주소를 좌표로 바꾼 결과.
 *
 * 지금은 deterministic mock 응답을 돌려준다.
 * 같은 주소는 항상 같은 좌표를 내리도록 지역 프로필 + hash 기반 보정치를 사용한다.
 */
@Schema(description = "주소를 좌표로 변환한 응답")
data class GeocodeResponse(
    @field:Schema(description = "입력된 주소 원문", example = "서울특별시 강남구 역삼동")
    val query: String,
    @field:Schema(description = "변환된 위도", example = "37.4979")
    val latitude: Double,
    @field:Schema(description = "변환된 경도", example = "127.0276")
    val longitude: Double,
    @field:Schema(description = "정규화된 주소", example = "서울특별시 강남구 역삼동")
    val normalizedAddress: String
)
