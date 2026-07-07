package com.example.mapservice.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("map-service API")
                .description("지도 좌표 해석, 지역 판별, 동네 인증을 제공하는 API")
                .version("v1")
        )
}
