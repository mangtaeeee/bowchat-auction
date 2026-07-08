package com.example.mapservice.region.dto.response

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 좌표를 행정동 기준 위치 정보로 해석한 응답 DTO.
 */
@Schema(description = "좌표 기반 지역 해석 응답")
data class ResolveRegionResponse(
    @field:Schema(description = "카카오 기준 지역 코드", example = "4113565500")
    val regionCode: String,
    @field:Schema(description = "전체 지역 이름", example = "경기도 성남시 분당구 삼평동")
    val regionName: String,
    @field:Schema(description = "시/도 이름", example = "경기도")
    val sidoName: String,
    @field:Schema(description = "시/군/구 이름", example = "성남시 분당구")
    val sigunguName: String,
    @field:Schema(description = "읍/면/동 이름", example = "삼평동")
    val emdName: String,
    @field:Schema(description = "해석에 사용된 위도", example = "37.402697")
    val latitude: Double,
    @field:Schema(description = "해석에 사용된 경도", example = "127.104599")
    val longitude: Double
)
