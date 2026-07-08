package com.example.mapservice.geocode.controller

import com.example.mapservice.geocode.dto.response.GeocodeResponse
import com.example.mapservice.geocode.dto.response.ReverseGeocodeResponse
import com.example.mapservice.geocode.service.GeocodeService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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

@Tag(name = "내부 지오코딩 API", description = "다른 내부 서비스가 호출하는 주소/좌표 변환 API")
@Validated
@RestController
@RequestMapping("/internal/maps")
class InternalGeocodeController(
    private val geocodeService: GeocodeService
) {

    @Operation(
        summary = "내부용 주소 좌표 변환",
        description = "다른 서비스가 주소 문자열을 전달하면 위도와 경도로 변환한다.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "내부 주소 변환 성공",
                content = [Content(schema = Schema(implementation = GeocodeResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "요청 파라미터 검증 실패",
                content = [Content(schema = Schema(implementation = com.example.mapservice.exception.ErrorResponse::class))]
            )
        ]
    )
    @GetMapping("/geocode")
    fun geocode(
        @Parameter(description = "정규화할 주소 문자열", example = "경기 성남시 분당구 판교역로 166")
        @RequestParam @NotBlank address: String
    ): GeocodeResponse =
        geocodeService.geocode(address)

    @Operation(
        summary = "내부용 역지오코딩",
        description = "다른 서비스가 좌표를 전달하면 주소와 지역 정보로 변환한다.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "내부 역지오코딩 성공",
                content = [Content(schema = Schema(implementation = ReverseGeocodeResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "요청 파라미터 검증 실패",
                content = [Content(schema = Schema(implementation = com.example.mapservice.exception.ErrorResponse::class))]
            )
        ]
    )
    @GetMapping("/reverse-geocode")
    fun reverseGeocode(
        @Parameter(description = "해석할 위치의 위도", example = "37.402697")
        @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") latitude: Double,
        @Parameter(description = "해석할 위치의 경도", example = "127.104599")
        @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") longitude: Double
    ): ReverseGeocodeResponse = geocodeService.reverseGeocode(latitude, longitude)
}
