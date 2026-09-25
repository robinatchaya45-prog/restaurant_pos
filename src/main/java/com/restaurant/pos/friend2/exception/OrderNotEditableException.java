package com.restaurant.pos.friend2.exception;

/** Thrown when a customer tries to edit/cancel an order after the 10-minute window has expired. */
public class OrderNotEditableException extends RuntimeException {
    public OrderNotEditableException(String message) {
        super(message);
    }
    public OrderNotEditableException(String message, Throwable cause) {
        super(message, cause);
    }
}