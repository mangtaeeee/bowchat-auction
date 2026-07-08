package com.example.mapservice.client

import com.fasterxml.jackson.annotation.JsonProperty
import com.example.mapservice.config.MapClientProperties
import com.example.mapservice.config.MapCacheNames
import com.example.mapservice.region.model.RegionInfo
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ResponseStatusException

@Component
@Profile("!test")
class KakaoMapProviderClient(
    private val mapWebClient: WebClient,
    private val properties: MapClientProperties
) : MapProviderClient {

    init {
        require(properties.apiKey.isNotBlank()) {
            "MAP_CLIENT_API_KEY must be configured for Kakao map provider"
        }
    }

    @Cacheable(cacheNames = [MapCacheNames.REGION], key = "T(com.example.mapservice.config.MapCacheKeys).coordinate(#latitude, #longitude)")
    override fun resolveRegion(latitude: Double, longitude: Double): RegionInfo {
        val response = mapWebClient.get()
            .uri { builder ->
                builder
                    .path("/v2/local/geo/coord2regioncode.json")
                    .queryParam("x", longitude)
                    .queryParam("y", latitude)
                    .queryParam("input_coord", "WGS84")
                    .build()
            }
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(KakaoCoord2RegionResponse::class.java)
            .block()
            ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "지도 공급자 응답이 비어 있습니다.")

        val region = response.documents.firstOrNull { it.regionType == "H" }
            ?: response.documents.firstOrNull()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "좌표에 해당하는 지역 정보를 찾을 수 없습니다.")

        return RegionInfo(
            regionCode = region.code,
            regionName = region.addressName,
            sidoName = region.region1DepthName,
            sigunguName = region.region2DepthName,
            emdName = listOf(region.region3DepthName, region.region4DepthName)
                .filter { it.isNotBlank() }
                .joinToString(" "),
            latitude = latitude,
            longitude = longitude
        )
    }

    @Cacheable(cacheNames = [MapCacheNames.GEOCODE], key = "T(com.example.mapservice.config.MapCacheKeys).address(#address)")
    override fun geocode(address: String): ProviderGeocodeResult {
        val response = mapWebClient.get()
            .uri { builder ->
                builder
                    .path("/v2/local/search/address.json")
                    .queryParam("query", address)
                    .queryParam("analyze_type", "similar")
                    .queryParam("size", 1)
                    .build()
            }
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(KakaoAddressSearchResponse::class.java)
            .block()
            ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "지도 공급자 응답이 비어 있습니다.")

        val document = response.documents.firstOrNull()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "주소에 해당하는 좌표를 찾을 수 없습니다.")

        return ProviderGeocodeResult(
            normalizedAddress = document.roadAddress?.addressName
                ?: document.address?.addressName
                ?: document.addressName,
            latitude = document.y.toDoubleOrNull()
                ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "지도 공급자 위도 응답이 올바르지 않습니다."),
            longitude = document.x.toDoubleOrNull()
                ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "지도 공급자 경도 응답이 올바르지 않습니다.")
        )
    }

    @Cacheable(cacheNames = [MapCacheNames.REVERSE_GEOCODE], key = "T(com.example.mapservice.config.MapCacheKeys).coordinate(#latitude, #longitude)")
    override fun reverseGeocode(latitude: Double, longitude: Double): ProviderReverseGeocodeResult {
        val response = mapWebClient.get()
            .uri { builder ->
                builder
                    .path("/v2/local/geo/coord2address.json")
                    .queryParam("x", longitude)
                    .queryParam("y", latitude)
                    .queryParam("input_coord", "WGS84")
                    .build()
            }
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(KakaoCoord2AddressResponse::class.java)
            .block()
            ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "지도 공급자 응답이 비어 있습니다.")

        val document = response.documents.firstOrNull()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "좌표에 해당하는 주소를 찾을 수 없습니다.")

        val region = resolveRegion(latitude, longitude)
        val resolvedAddress = document.roadAddress?.addressName
            ?: document.address?.addressName
            ?: region.regionName

        return ProviderReverseGeocodeResult(
            address = resolvedAddress,
            regionCode = region.regionCode,
            regionName = region.regionName
        )
    }

    private data class KakaoAddressSearchResponse(
        val documents: List<KakaoAddressSearchDocument> = emptyList()
    )

    private data class KakaoAddressSearchDocument(
        @JsonProperty("address_name")
        val addressName: String = "",
        val x: String = "",
        val y: String = "",
        val address: KakaoAddressDetail? = null,
        @JsonProperty("road_address")
        val roadAddress: KakaoRoadAddressDetail? = null
    )

    private data class KakaoCoord2RegionResponse(
        val documents: List<KakaoRegionDocument> = emptyList()
    )

    private data class KakaoRegionDocument(
        @JsonProperty("region_type")
        val regionType: String = "",
        @JsonProperty("address_name")
        val addressName: String = "",
        @JsonProperty("region_1depth_name")
        val region1DepthName: String = "",
        @JsonProperty("region_2depth_name")
        val region2DepthName: String = "",
        @JsonProperty("region_3depth_name")
        val region3DepthName: String = "",
        @JsonProperty("region_4depth_name")
        val region4DepthName: String = "",
        val code: String = ""
    )

    private data class KakaoCoord2AddressResponse(
        val documents: List<KakaoCoord2AddressDocument> = emptyList()
    )

    private data class KakaoCoord2AddressDocument(
        val address: KakaoAddressDetail? = null,
        @JsonProperty("road_address")
        val roadAddress: KakaoRoadAddressDetail? = null
    )

    private data class KakaoAddressDetail(
        @JsonProperty("address_name")
        val addressName: String = ""
    )

    private data class KakaoRoadAddressDetail(
        @JsonProperty("address_name")
        val addressName: String = ""
    )
}
