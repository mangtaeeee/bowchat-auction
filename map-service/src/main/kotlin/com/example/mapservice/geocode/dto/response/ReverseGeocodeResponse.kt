package com.example.mapservice.geocode.dto.response

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 좌표를 다시 주소/동네 이름으로 해석한 결과.
 */
@Schema(description = "좌표를 주소로 역변환한 응답")
data class ReverseGeocodeResponse(
    @field:Schema(description = "입력 위도", example = "37.5665")
    val latitude: Double,
    @field:Schema(description = "입력 경도", example = "126.9780")
    val longitude: Double,
    @field:Schema(description = "해석된 주소", example = "서울특별시 강남구 역삼동")
    val address: String,
    @field:Schema(description = "해석된 지역 코드", example = "KR-SEOUL-GANGNAM-01")
    val regionCode: String,
    @field:Schema(description = "해석된 지역 이름", example = "서울특별시 강남구 역삼동")
    val regionName: String
)
