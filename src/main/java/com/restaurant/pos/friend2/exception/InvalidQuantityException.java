package com.restaurant.pos.friend2.exception;

/** Thrown when an order item quantity is missing, zero, or negative. */
public class InvalidQuantityException extends RuntimeException {
    public InvalidQuantityException(String message) {
        super(message);
    }
    public InvalidQuantityException(String message, Throwable cause) {
        super(message, cause);
    }
}