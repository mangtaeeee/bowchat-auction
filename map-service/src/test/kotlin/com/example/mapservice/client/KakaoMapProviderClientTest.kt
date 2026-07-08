package com.example.mapservice.client

import com.example.mapservice.config.MapClientProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

class KakaoMapProviderClientTest {

    @Test
    fun `geocode maps kakao address search response`() {
        val client = providerClient(
            """
                {
                  "documents": [
                    {
                      "address_name": "서울 강남구 역삼동 123-4",
                      "x": "127.0276",
                      "y": "37.4979",
                      "address": {
                        "address_name": "서울 강남구 역삼동 123-4"
                      },
                      "road_address": {
                        "address_name": "서울 강남구 테헤란로 123"
                      }
                    }
                  ]
                }
            """.trimIndent()
        )

        val result = client.geocode("서울특별시 강남구 역삼동")

        assertThat(result.normalizedAddress).isEqualTo("서울 강남구 테헤란로 123")
        assertThat(result.latitude).isEqualTo(37.4979)
        assertThat(result.longitude).isEqualTo(127.0276)
    }

    @Test
    fun `resolve region prefers hangjeong region type`() {
        val client = providerClient(
            """
                {
                  "documents": [
                    {
                      "region_type": "B",
                      "address_name": "경기도 성남시 분당구 삼평동",
                      "region_1depth_name": "경기도",
                      "region_2depth_name": "성남시 분당구",
                      "region_3depth_name": "삼평동",
                      "region_4depth_name": "",
                      "code": "4113510900"
                    },
                    {
                      "region_type": "H",
                      "address_name": "경기도 성남시 분당구 삼평동",
                      "region_1depth_name": "경기도",
                      "region_2depth_name": "성남시 분당구",
                      "region_3depth_name": "삼평동",
                      "region_4depth_name": "",
                      "code": "4113565500"
                    }
                  ]
                }
            """.trimIndent()
        )

        val result = client.resolveRegion(37.40269721785548, 127.10459896729914)

        assertThat(result.regionCode).isEqualTo("4113565500")
        assertThat(result.regionName).isEqualTo("경기도 성남시 분당구 삼평동")
        assertThat(result.emdName).isEqualTo("삼평동")
    }

    @Test
    fun `reverse geocode uses coord2address and region lookup`() {
        val webClient = WebClient.builder()
            .exchangeFunction(SequentialExchangeFunction(
                listOf(
                    jsonResponse(
                        """
                            {
                              "documents": [
                                {
                                  "address": {
                                    "address_name": "경기 성남시 분당구 삼평동 681"
                                  },
                                  "road_address": {
                                    "address_name": "경기 성남시 분당구 판교역로 166"
                                  }
                                }
                              ]
                            }
                        """.trimIndent()
                    ),
                    jsonResponse(
                        """
                            {
                              "documents": [
                                {
                                  "region_type": "H",
                                  "address_name": "경기도 성남시 분당구 삼평동",
                                  "region_1depth_name": "경기도",
                                  "region_2depth_name": "성남시 분당구",
                                  "region_3depth_name": "삼평동",
                                  "region_4depth_name": "",
                                  "code": "4113565500"
                                }
                              ]
                            }
                        """.trimIndent()
                    )
                )
            ))
            .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK test-key")
            .build()

        val client = KakaoMapProviderClient(webClient, MapClientProperties(apiKey = "test-key"))

        val result = client.reverseGeocode(37.40269721785548, 127.10459896729914)

        assertThat(result.address).isEqualTo("경기 성남시 분당구 판교역로 166")
        assertThat(result.regionCode).isEqualTo("4113565500")
        assertThat(result.regionName).isEqualTo("경기도 성남시 분당구 삼평동")
    }

    private fun providerClient(body: String): KakaoMapProviderClient {
        val webClient = WebClient.builder()
            .exchangeFunction { request ->
                assertThat(request.headers().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("KakaoAK test-key")
                Mono.just(jsonResponse(body))
            }
            .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK test-key")
            .build()

        return KakaoMapProviderClient(webClient, MapClientProperties(apiKey = "test-key"))
    }

    private fun jsonResponse(body: String): ClientResponse =
        ClientResponse.create(HttpStatus.OK)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(body)
            .build()

    private class SequentialExchangeFunction(
        responses: List<ClientResponse>
    ) : ExchangeFunction {

        private val iterator = responses.iterator()

        override fun exchange(request: ClientRequest): Mono<ClientResponse> {
            assertThat(request.headers().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("KakaoAK test-key")
            return Mono.just(iterator.next())
        }
    }
}
