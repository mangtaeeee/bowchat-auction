package com.example.mapservice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@ConfigurationProperties(prefix = "map.client")
data class MapClientProperties(
    var baseUrl: String = "",
    var apiKey: String = ""
)

@Configuration
@EnableConfigurationProperties(MapClientProperties::class)
class WebClientConfig {

    @Bean
    fun mapWebClient(builder: WebClient.Builder, properties: MapClientProperties): WebClient {
        val configuredBuilder = if (properties.baseUrl.isBlank()) builder else builder.baseUrl(properties.baseUrl)
        return configuredBuilder.build()
    }
}
