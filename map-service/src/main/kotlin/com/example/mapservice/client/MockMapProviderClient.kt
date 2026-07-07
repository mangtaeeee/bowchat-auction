package com.example.mapservice.client

import com.example.mapservice.region.model.RegionInfo
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import kotlin.math.abs
import kotlin.math.pow

/**
 * 외부 지도 API를 아직 붙이지 않은 동안 사용하는 임시 provider.
 *
 * 단순 hash만 쓰던 초기 버전보다 한 단계 더 나아가,
 * 주소 안의 지역 단서를 읽어 그럴듯한 좌표/지역 정보를 돌려주도록 만든 mock 구현이다.
 *
 * 목적:
 * 1. 같은 입력이면 항상 같은 출력이 나온다.
 * 2. 한국 지역 거래 서비스처럼 보이는 주소/동네 응답을 준다.
 * 3. 실제 Kakao/Naver client로 교체할 때 인터페이스는 유지한다.
 */
@Component
@Profile("dev | test")
class MockMapProviderClient : MapProviderClient {

    override fun resolveRegion(latitude: Double, longitude: Double): RegionInfo {
        val profile = nearestProfile(latitude, longitude)
        val neighborhoodIndex = neighborhoodIndex(latitude, longitude, profile.neighborhoods.size)
        val neighborhood = profile.neighborhoods[neighborhoodIndex]

        return RegionInfo(
            regionCode = buildRegionCode(profile, neighborhoodIndex),
            regionName = buildRegionName(profile, neighborhood),
            sidoName = profile.sidoName,
            sigunguName = profile.sigunguName,
            emdName = neighborhood,
            latitude = latitude,
            longitude = longitude
        )
    }

    override fun geocode(address: String): ProviderGeocodeResult {
        val normalizedAddress = normalizeAddress(address)
        val profile = detectProfile(normalizedAddress)
        val neighborhood = detectNeighborhood(profile, normalizedAddress)
        val seed = normalizedAddress.hashCode().toUInt().toLong()

        val latitude = profile.centerLatitude + offset(seed, 0)
        val longitude = profile.centerLongitude + offset(seed, 1)

        return ProviderGeocodeResult(
            normalizedAddress = normalizeResolvedAddress(profile, neighborhood),
            latitude = latitude,
            longitude = longitude
        )
    }

    override fun reverseGeocode(latitude: Double, longitude: Double): ProviderReverseGeocodeResult {
        val resolved = resolveRegion(latitude, longitude)
        return ProviderReverseGeocodeResult(
            address = resolved.regionName,
            regionCode = resolved.regionCode,
            regionName = resolved.regionName
        )
    }

    private fun detectProfile(address: String): RegionProfile {
        return REGION_PROFILES.firstOrNull { profile ->
            address.contains(profile.sigunguName) || profile.neighborhoods.any(address::contains)
        } ?: REGION_PROFILES[address.hashCode().toUInt().toInt() % REGION_PROFILES.size]
    }

    private fun detectNeighborhood(profile: RegionProfile, address: String): String {
        return profile.neighborhoods.firstOrNull(address::contains)
            ?: profile.neighborhoods[address.hashCode().toUInt().toInt() % profile.neighborhoods.size]
    }

    private fun nearestProfile(latitude: Double, longitude: Double): RegionProfile {
        return REGION_PROFILES.minBy { profile ->
            (latitude - profile.centerLatitude).pow(2) + (longitude - profile.centerLongitude).pow(2)
        }
    }

    private fun neighborhoodIndex(latitude: Double, longitude: Double, size: Int): Int {
        val latSeed = abs((latitude * 1000).toInt())
        val lonSeed = abs((longitude * 1000).toInt())
        return (latSeed + lonSeed) % size
    }

    /**
     * 중심 좌표에서 너무 멀어지지 않도록 최대 약 500m 안쪽 정도의 작은 오프셋만 준다.
     */
    private fun offset(seed: Long, shift: Int): Double {
        val sliced = (seed shr (shift * 8)).toInt() and 0xFF
        return ((sliced % 11) - 5) / 1000.0
    }

    private fun normalizeAddress(address: String): String =
        address.trim().replace("\\s+".toRegex(), " ")

    private fun normalizeResolvedAddress(profile: RegionProfile, neighborhood: String): String =
        "${profile.sidoName} ${profile.sigunguName} $neighborhood"

    private fun buildRegionName(profile: RegionProfile, neighborhood: String): String =
        normalizeResolvedAddress(profile, neighborhood)

    private fun buildRegionCode(profile: RegionProfile, neighborhoodIndex: Int): String =
        "${profile.regionCodePrefix}-${(neighborhoodIndex + 1).toString().padStart(2, '0')}"

    private data class RegionProfile(
        val regionCodePrefix: String,
        val sidoName: String,
        val sigunguName: String,
        val centerLatitude: Double,
        val centerLongitude: Double,
        val neighborhoods: List<String>
    )

    companion object {
        private val REGION_PROFILES = listOf(
            RegionProfile(
                regionCodePrefix = "KR-SEOUL-GANGNAM",
                sidoName = "서울특별시",
                sigunguName = "강남구",
                centerLatitude = 37.4979,
                centerLongitude = 127.0276,
                neighborhoods = listOf("역삼동", "삼성동", "대치동", "청담동", "논현동")
            ),
            RegionProfile(
                regionCodePrefix = "KR-SEOUL-SONGPA",
                sidoName = "서울특별시",
                sigunguName = "송파구",
                centerLatitude = 37.5145,
                centerLongitude = 127.1059,
                neighborhoods = listOf("잠실동", "석촌동", "가락동", "문정동", "방이동")
            ),
            RegionProfile(
                regionCodePrefix = "KR-SEOUL-MAPO",
                sidoName = "서울특별시",
                sigunguName = "마포구",
                centerLatitude = 37.5563,
                centerLongitude = 126.9086,
                neighborhoods = listOf("합정동", "망원동", "상암동", "서교동", "연남동")
            ),
            RegionProfile(
                regionCodePrefix = "KR-SEOUL-SEONGDONG",
                sidoName = "서울특별시",
                sigunguName = "성동구",
                centerLatitude = 37.5633,
                centerLongitude = 127.0369,
                neighborhoods = listOf("성수동", "행당동", "금호동", "옥수동", "왕십리동")
            ),
            RegionProfile(
                regionCodePrefix = "KR-GYEONGGI-BUNDANG",
                sidoName = "경기도 성남시",
                sigunguName = "분당구",
                centerLatitude = 37.3826,
                centerLongitude = 127.1187,
                neighborhoods = listOf("정자동", "서현동", "야탑동", "판교동", "이매동")
            )
        )
    }
}
