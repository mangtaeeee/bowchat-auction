package com.example.auctionservice.exception;

import com.example.auctionservice.entity.AuctionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // AuctionException (판매자 입찰, 최고가 이하 등)
    @ExceptionHandler(AuctionException.class)
    public ResponseEntity<ErrorResponse> handleAuctionException(AuctionException e) {
        log.warn("AuctionException: {}", e.getMessage());
        return ResponseEntity
                .status(e.getStatusCode())
                .body(new ErrorResponse(e.getMessage()));
    }

    // ResponseStatusException (404, 503 등)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException e) {
        log.warn("ResponseStatusException: {}", e.getMessage());
        return ResponseEntity
                .status(e.getStatusCode())
                .body(new ErrorResponse(e.getReason()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unhandled Exception: ", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("서버 오류가 발생했습니다."));
    }

    public record ErrorResponse(String message) {}
}