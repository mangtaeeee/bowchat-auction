package com.example.mapservice.geocode.dto.response

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 좌표를 다시 주소/동네 이름으로 해석한 결과.
 */
@Schema(description = "좌표를 주소로 역변환한 응답")
data class ReverseGeocodeResponse(
    @field:Schema(description = "입력 위도", example = "37.402697")
    val latitude: Double,
    @field:Schema(description = "입력 경도", example = "127.104599")
    val longitude: Double,
    @field:Schema(description = "좌표를 기준으로 해석된 대표 주소", example = "경기 성남시 분당구 판교역로 166")
    val address: String,
    @field:Schema(description = "해석된 행정동 지역 코드", example = "4113565500")
    val regionCode: String,
    @field:Schema(description = "해석된 행정동 지역 이름", example = "경기도 성남시 분당구 삼평동")
    val regionName: String
)
