package com.example.mapservice.client

/**
 * 외부 지도 provider에서 좌표 변환 결과를 받아올 때 쓰는 값 객체.
 */
data class ProviderGeocodeResult(
    val normalizedAddress: String,
    val latitude: Double,
    val longitude: Double
)
