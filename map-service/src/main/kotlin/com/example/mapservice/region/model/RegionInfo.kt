package com.example.mapservice.region.model

/**
 * Kotlin의 [data class]는 Java의 `record`와 비슷한 역할을 한다.
 *
 * 이 타입은 지역 해석 결과를 서비스 내부에서 넘길 때 쓰는 값 객체다.
 * 현재는 외부 지도 API를 아직 붙이지 않았기 때문에 임시 region code와 region name을 담는다.
 */
data class RegionInfo(
    val regionCode: String,
    val regionName: String,
    val sidoName: String,
    val sigunguName: String,
    val emdName: String,
    val latitude: Double,
    val longitude: Double
)
