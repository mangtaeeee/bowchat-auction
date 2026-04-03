package com.example.auctionservice.exception;

import com.example.auctionservice.entity.AuctionException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * AuctionException
     * 판매자 입찰 방지, 최고가 이하, 경매 종료 등 경매 도메인 예외
     * 어떤 경매에서 발생했는지 컨텍스트 포함 가능
     */
    @ExceptionHandler(AuctionException.class)
    public ResponseEntity<ErrorResponse> handleAuctionException(AuctionException e) {
        log.warn("AuctionException: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(ErrorResponse.of(
                        e.getErrorCode().getCode(),
                        e.getMessage()
                ));
    }

    /**
     * ResponseStatusException
     * 404, 403, 503 등 직접 던진 예외
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException e) {
        log.warn("ResponseStatusException: status={}, reason={}", e.getStatusCode(), e.getReason());
        return ResponseEntity
                .status(e.getStatusCode())
                .body(ErrorResponse.of(
                        e.getStatusCode().toString(),
                        e.getReason()
                ));
    }

    /**
     * @Valid 검증 실패
     * 어떤 필드가 왜 실패했는지 컨텍스트 포함
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, Object> context = new HashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(error -> context.put(error.getField(), error.getDefaultMessage()));

        log.warn("Validation 실패: {}", context);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("VALIDATION_ERROR", "요청 값이 올바르지 않습니다.", context));
    }

    /**
     * FeignClient 호출 실패
     */
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(FeignException e) {
        log.error("FeignClient 호출 실패: status={}, message={}", e.status(), e.getMessage());

        if (e.status() == 404) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.of("NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."));
        }

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of("SERVICE_UNAVAILABLE", "일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해주세요."));
    }

    /**
     * 그 외 처리되지 않은 예외
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unhandled Exception: ", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."));
    }
}