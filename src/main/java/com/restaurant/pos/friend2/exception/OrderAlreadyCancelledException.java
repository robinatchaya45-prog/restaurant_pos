package com.restaurant.pos.friend2.exception;

/** Thrown when an operation is attempted on an order that is already cancelled. */
public class OrderAlreadyCancelledException extends RuntimeException {
    public OrderAlreadyCancelledException(String message) {
        super(message);
    }
    public OrderAlreadyCancelledException(String message, Throwable cause) {
        super(message, cause);
    }
}