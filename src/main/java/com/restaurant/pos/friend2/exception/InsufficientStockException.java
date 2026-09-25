package com.restaurant.pos.friend2.exception;

/** Thrown when there is not enough available ingredient stock to reserve. */
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
    public InsufficientStockException(String message, Throwable cause) {
        super(message, cause);
    }
}