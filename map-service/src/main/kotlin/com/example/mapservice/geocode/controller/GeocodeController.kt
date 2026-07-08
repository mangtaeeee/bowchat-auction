package com.example.mapservice.geocode.controller

import com.example.mapservice.geocode.dto.response.GeocodeResponse
import com.example.mapservice.geocode.dto.response.ReverseGeocodeResponse
import com.example.mapservice.geocode.service.GeocodeService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 외부 클라이언트용 주소/좌표 변환 API.
 */
@Tag(name = "지오코딩 API", description = "외부 클라이언트가 사용하는 주소/좌표 변환 API")
@Validated
@RestController
@RequestMapping
class GeocodeController(
    private val geocodeService: GeocodeService
) {

    @Operation(
        summary = "주소를 좌표로 변환",
        description = "주소 문자열을 위도와 경도로 변환한다.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "주소 변환 성공",
                content = [Content(schema = Schema(implementation = GeocodeResponse::class))]
            )
        ]
    )
    @GetMapping("/geocode")
    fun geocode(@RequestParam @NotBlank address: String): GeocodeResponse =
        geocodeService.geocode(address)

    @Operation(
        summary = "좌표를 주소로 변환",
        description = "위도와 경도를 받아 사람이 읽을 수 있는 주소와 지역 정보로 변환한다.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "역지오코딩 성공",
                content = [Content(schema = Schema(implementation = ReverseGeocodeResponse::class))]
            )
        ]
    )
    @GetMapping("/reverse-geocode")
    fun reverseGeocode(
        @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") latitude: Double,
        @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") longitude: Double
    ): ReverseGeocodeResponse = geocodeService.reverseGeocode(latitude, longitude)
}
