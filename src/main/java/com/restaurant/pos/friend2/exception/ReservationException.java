package com.restaurant.pos.friend2.exception;

/** Generic exception for failures during stock reservation / release. */
public class ReservationException extends RuntimeException {
    public ReservationException(String message) {
        super(message);
    }
    public ReservationException(String message, Throwable cause) {
        super(message, cause);
    }
}