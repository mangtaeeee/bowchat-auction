package com.example.mapservice.region.dto.response

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 좌표를 행정동 비슷한 결과로 해석한 응답 DTO.
 *
 * 현재 regionCode는 임시 계산값이며, 실제 지도 API 연동 시 외부 표준 코드로 교체한다.
 */
@Schema(description = "좌표 기반 지역 해석 응답")
data class ResolveRegionResponse(
    @field:Schema(description = "지역 코드", example = "KR-SEOUL-GANGNAM-01")
    val regionCode: String,
    @field:Schema(description = "지역 이름", example = "서울특별시 강남구 역삼동")
    val regionName: String,
    @field:Schema(description = "시/도 이름", example = "서울특별시")
    val sidoName: String,
    @field:Schema(description = "시/군/구 이름", example = "강남구")
    val sigunguName: String,
    @field:Schema(description = "읍/면/동 이름", example = "역삼동")
    val emdName: String,
    @field:Schema(description = "해석에 사용된 위도", example = "37.5665")
    val latitude: Double,
    @field:Schema(description = "해석에 사용된 경도", example = "126.9780")
    val longitude: Double
)
