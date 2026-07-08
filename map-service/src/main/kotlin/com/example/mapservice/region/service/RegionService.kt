package com.example.mapservice.region.service

import com.example.mapservice.client.MapProviderClient
import com.example.mapservice.region.dto.response.VerifyRegionResponse
import com.example.mapservice.region.model.RegionInfo
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * 좌표를 지역 정보로 해석하는 서비스.
 *
 * 다른 서비스와의 경계는 단순하다.
 * - user-service, product-service는 이 서비스를 호출한다.
 * - map-service는 좌표와 주소를 해석한 결과만 돌려준다.
 *
 * 실제 provider 연동은 [MapProviderClient]로 분리해 두었고,
 * 현재는 MockMapProviderClient가 deterministic placeholder를 제공한다.
 */
@Service
class RegionService(
    private val mapProviderClient: MapProviderClient
) {

    fun resolve(latitude: Double, longitude: Double): RegionInfo =
        mapProviderClient.resolveRegion(latitude, longitude)

    fun verify(userId: Long, latitude: Double, longitude: Double, expectedRegionCode: String): VerifyRegionResponse {
        val resolved = resolve(latitude, longitude)
        return VerifyRegionResponse(
            userId = userId,
            verified = resolved.regionCode == expectedRegionCode,
            resolvedRegionCode = resolved.regionCode,
            resolvedRegionName = resolved.regionName,
            verifiedAt = Instant.now()
        )
    }
}
