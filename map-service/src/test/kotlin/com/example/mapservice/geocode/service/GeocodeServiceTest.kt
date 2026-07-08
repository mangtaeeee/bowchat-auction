package com.example.mapservice.geocode.service

import com.example.mapservice.client.MockMapProviderClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GeocodeServiceTest {

    private val geocodeService = GeocodeService(MockMapProviderClient())

    @Test
    fun `같은 주소는 항상 같은 좌표를 반환한다`() {
        val first = geocodeService.geocode("서울특별시 강남구 역삼동")
        val second = geocodeService.geocode("서울특별시 강남구 역삼동")

        assertThat(first.latitude).isEqualTo(second.latitude)
        assertThat(first.longitude).isEqualTo(second.longitude)
        assertThat(first.normalizedAddress).isEqualTo("서울특별시 강남구 역삼동")
    }

    @Test
    fun `좌표를 역지오코딩하면 한국형 주소를 반환한다`() {
        val result = geocodeService.reverseGeocode(37.4980, 127.0277)

        assertThat(result.address).contains("서울특별시")
        assertThat(result.regionName).contains("강남구")
        assertThat(result.regionCode).startsWith("KR-SEOUL-GANGNAM")
    }
}
