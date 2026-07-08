package com.example.mapservice.geocode.controller

import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest(
    properties = [
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
    ]
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GeocodeControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `reverse geocode invalid latitude returns validation error response`() {
        mockMvc.get("/reverse-geocode") {
            param("latitude", "91.0")
            param("longitude", "127.0276")
        }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("VALIDATION_ERROR") }
                jsonPath("$.message") { value("요청 값이 올바르지 않습니다.") }
                jsonPath("$.context.latitude", containsString("90.0"))
            }
    }

    @Test
    fun `geocode returns deterministic address payload`() {
        mockMvc.get("/geocode") {
            param("address", "서울특별시 강남구 역삼동")
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.query") { value("서울특별시 강남구 역삼동") }
                jsonPath("$.normalizedAddress") { value("서울특별시 강남구 역삼동") }
            }
    }
}
