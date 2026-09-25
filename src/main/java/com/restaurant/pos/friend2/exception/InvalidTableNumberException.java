package com.restaurant.pos.friend2.exception;

/** Thrown when the table number is empty, non-numeric, or not positive. */
public class InvalidTableNumberException extends RuntimeException {
    public InvalidTableNumberException(String message) {
        super(message);
    }
    public InvalidTableNumberException(String message, Throwable cause) {
        super(message, cause);
    }
}