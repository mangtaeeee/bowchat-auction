package com.example.mapservice.client

/**
 * 외부 지도 provider에서 역지오코딩 결과를 받아올 때 쓰는 값 객체.
 */
data class ProviderReverseGeocodeResult(
    val address: String,
    val regionCode: String,
    val regionName: String
)
