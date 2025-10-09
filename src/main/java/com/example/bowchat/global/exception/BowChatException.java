package com.example.bowchat.global.exception;

import org.springframework.http.HttpStatus;

public abstract class BowChatException extends RuntimeException{
    public BowChatException(String message) {
        super(message);
    }
    public abstract HttpStatus getStatus();
    public abstract String getCode();
}