package com.example.eCommerceUdemy.exception;

import org.springframework.http.HttpStatus;

public class TokenExpireException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private HttpStatus status;

    public TokenExpireException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
