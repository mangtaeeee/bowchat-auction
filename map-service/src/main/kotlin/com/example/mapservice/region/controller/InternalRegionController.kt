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

@Tag(name = "내부 지역 API", description = "다른 내부 서비스가 호출하는 지역 해석 및 동네 인증 API")
@RestController
@RequestMapping("/internal/maps")
class InternalRegionController(
    private val regionService: RegionService
) {

    @Operation(
        summary = "내부용 좌표 지역 해석",
        description = "다른 서비스가 위도와 경도를 전달하면 지역 코드와 지역 이름을 반환한다.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "내부 지역 해석 성공",
                content = [Content(schema = Schema(implementation = ResolveRegionResponse::class))]
            )
        ]
    )
    @PostMapping("/regions/resolve")
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
        summary = "내부용 동네 인증",
        description = "다른 서비스가 사용자 좌표와 기대 지역 코드를 전달하면 일치 여부를 검증한다.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "내부 동네 인증 성공",
                content = [Content(schema = Schema(implementation = VerifyRegionResponse::class))]
            )
        ]
    )
    @PostMapping("/regions/verify")
    fun verify(@Valid @RequestBody request: VerifyRegionRequest): VerifyRegionResponse =
        regionService.verify(
            userId = request.userId,
            latitude = request.latitude,
            longitude = request.longitude,
            expectedRegionCode = request.expectedRegionCode
        )
}
