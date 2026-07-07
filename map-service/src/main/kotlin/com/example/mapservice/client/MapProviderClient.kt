package com.example.mapservice.client

import com.example.mapservice.region.model.RegionInfo

/**
 * 실제 지도 provider와 통신하는 클라이언트 추상화.
 *
 * 현재는 mock 구현만 두고, 나중에 Kakao/Naver client가 이 인터페이스를 구현하도록 만든다.
 */
interface MapProviderClient {

    fun resolveRegion(latitude: Double, longitude: Double): RegionInfo

    fun geocode(address: String): ProviderGeocodeResult

    fun reverseGeocode(latitude: Double, longitude: Double): ProviderReverseGeocodeResult
}
