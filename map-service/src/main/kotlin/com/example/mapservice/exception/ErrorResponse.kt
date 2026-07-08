package com.example.mapservice.exception

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "공통 오류 응답")
data class ErrorResponse(
    @field:Schema(description = "오류 코드", example = "VALIDATION_ERROR")
    val code: String,
    @field:Schema(description = "오류 메시지", example = "요청 값이 올바르지 않습니다.")
    val message: String,
    @field:Schema(description = "필드별 검증 오류나 추가 진단 정보를 담는 컨텍스트", example = """{"latitude":"90.0 이하여야 합니다."}""")
    val context: Map<String, Any>? = null
) {
    companion object {
        fun of(code: String, message: String): ErrorResponse = ErrorResponse(code, message)

        fun of(code: String, message: String, context: Map<String, Any>): ErrorResponse =
            ErrorResponse(code, message, context)
    }
}
