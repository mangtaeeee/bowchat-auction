package com.example.mapservice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient

@ConfigurationProperties(prefix = "map.client")
data class MapClientProperties(
    var baseUrl: String = "https://dapi.kakao.com",
    var apiKey: String = ""
)

@Configuration
@EnableConfigurationProperties(MapClientProperties::class)
class WebClientConfig {

    @Bean
    fun mapWebClient(builder: WebClient.Builder, properties: MapClientProperties): WebClient {
        return builder
            .baseUrl(properties.baseUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK ${properties.apiKey}")
            .build()
    }
}
