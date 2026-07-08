package com.example.mapservice.region.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@SpringBootTest(
    properties = [
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
    ]
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegionControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `resolve returns region fields`() {
        mockMvc.post("/regions/resolve") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "latitude": 37.4979,
                  "longitude": 127.0276
                }
            """.trimIndent()
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.regionCode") { value("KR-SEOUL-GANGNAM-05") }
                jsonPath("$.regionName") { value("서울특별시 강남구 논현동") }
                jsonPath("$.emdName") { value("논현동") }
            }
    }

    @Test
    fun `verify invalid request body returns validation error response`() {
        mockMvc.post("/regions/verify") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "userId": 1,
                  "latitude": 37.4979,
                  "longitude": 127.0276,
                  "expectedRegionCode": ""
                }
            """.trimIndent()
        }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("VALIDATION_ERROR") }
                jsonPath("$.context.expectedRegionCode") { exists() }
            }
    }
}
