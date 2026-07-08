package com.example.mapservice.region.controller

import com.example.mapservice.region.dto.request.ResolveRegionRequest
import com.example.mapservice.region.dto.request.VerifyRegionRequest
import com.example.mapservice.region.dto.response.ResolveRegionResponse
import com.example.mapservice.region.dto.response.VerifyRegionResponse
import com.example.mapservice.region.service.RegionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 외부 클라이언트용 지역 해석/검증 API.
 */
@Tag(name = "지역 해석 API", description = "외부 클라이언트가 사용하는 지역 해석 및 동네 인증 API")
@RestController
@RequestMapping("/regions")
class RegionController(
    private val regionService: RegionService
) {

    @Operation(
        summary = "좌표로 지역 해석",
        description = "위도와 경도를 받아 현재 좌표를 지역 코드와 지역 이름으로 해석한다.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "지역 해석 성공",
                content = [Content(schema = Schema(implementation = ResolveRegionResponse::class))]
            )
        ]
    )
    @PostMapping("/resolve")
    fun resolve(@Valid @RequestBody request: ResolveRegionRequest): ResolveRegionResponse {
        val region = regionService.resolve(request.latitude, request.longitude)
        return ResolveRegionResponse(
            regionCode = region.regionCode,
            regionName = region.regionName,
            sidoName = region.sidoName,
            sigunguName = region.sigunguName,
            emdName = region.emdName,
            latitude = region.latitude,
            longitude = region.longitude
        )
    }

    @Operation(
        summary = "동네 인증",
        description = "현재 좌표가 기대한 지역 코드와 일치하는지 검증한다.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "동네 인증 처리 성공",
                content = [Content(schema = Schema(implementation = VerifyRegionResponse::class))]
            )
        ]
    )
    @PostMapping("/verify")
    fun verify(@Valid @RequestBody request: VerifyRegionRequest): VerifyRegionResponse =
        regionService.verify(
            userId = request.userId,
            latitude = request.latitude,
            longitude = request.longitude,
            expectedRegionCode = request.expectedRegionCode
        )
}
