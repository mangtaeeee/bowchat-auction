package com.example.mapservice.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

/**
 * 기존 서비스들과 같은 형태의 전역 예외 처리기.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException): ResponseEntity<ErrorResponse> {
        log.warn("ResponseStatusException: status={}, reason={}", ex.statusCode, ex.reason)
        return ResponseEntity
            .status(ex.statusCode)
            .body(
                ErrorResponse.of(
                    ex.statusCode.toString(),
                    ex.reason ?: "요청 처리 중 오류가 발생했습니다."
                )
            )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val context = linkedMapOf<String, Any>()
        ex.bindingResult.fieldErrors.forEach { error ->
            context[error.field] = error.defaultMessage ?: "유효하지 않은 값입니다."
        }

        log.warn("Validation 실패: {}", context)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of("VALIDATION_ERROR", "요청 값이 올바르지 않습니다.", context))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error("처리되지 않은 예외가 발생했습니다.", ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.of("INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."))
    }
}
