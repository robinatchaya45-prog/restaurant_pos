package com.restaurant.pos.friend2.exception;

/** Wraps low-level SQLException / connection failures into an unchecked exception. */
public class DatabaseException extends RuntimeException {
    public DatabaseException(String message) {
        super(message);
    }
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}