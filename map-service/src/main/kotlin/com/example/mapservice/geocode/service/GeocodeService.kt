package com.example.mapservice.geocode.service

import com.example.mapservice.client.MapProviderClient
import com.example.mapservice.geocode.dto.response.GeocodeResponse
import com.example.mapservice.geocode.dto.response.ReverseGeocodeResponse
import org.springframework.stereotype.Service

/**
 * 주소/좌표 변환 서비스.
 *
 * map-service는 다른 서비스처럼 상위 도메인 데이터를 들고 있지 않고,
 * 위치 해석 결과만 반환하는 utility service 역할을 맡는다.
 */
@Service
class GeocodeService(
    private val mapProviderClient: MapProviderClient
) {

    fun geocode(address: String): GeocodeResponse {
        val result = mapProviderClient.geocode(address)
        return GeocodeResponse(
            query = address,
            latitude = result.latitude,
            longitude = result.longitude,
            normalizedAddress = result.normalizedAddress
        )
    }

    fun reverseGeocode(latitude: Double, longitude: Double): ReverseGeocodeResponse {
        val resolved = mapProviderClient.reverseGeocode(latitude, longitude)
        return ReverseGeocodeResponse(
            latitude = latitude,
            longitude = longitude,
            address = resolved.address,
            regionCode = resolved.regionCode,
            regionName = resolved.regionName
        )
    }
}
