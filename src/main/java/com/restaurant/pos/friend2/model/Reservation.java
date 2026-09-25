package com.restaurant.pos.friend2.model;

import java.math.BigDecimal;

/**
 * Plain domain model representing a temporary stock reservation
 * held against an Order for a given ingredient. ingredientId
 * references Friend 1's Ingredient primary key, but this class
 * does NOT depend on Friend 1's Ingredient class (loose coupling).
 */
public class Reservation {

    private Long reservationId;
    private Long orderId;
    private Long ingredientId;
    private BigDecimal reservedQuantity;
    private ReservationStatus status;

    public Reservation() {
        this.status = ReservationStatus.ACTIVE;
    }

    public Reservation(Long reservationId, Long orderId, Long ingredientId,
                        BigDecimal reservedQuantity, ReservationStatus status) {
        this.reservationId = reservationId;
        this.orderId = orderId;
        setIngredientId(ingredientId);
        setReservedQuantity(reservedQuantity);
        this.status = status == null ? ReservationStatus.ACTIVE : status;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getIngredientId() {
        return ingredientId;
    }

    public void setIngredientId(Long ingredientId) {
        if (ingredientId == null) {
            throw new IllegalArgumentException("ingredientId must not be null");
        }
        this.ingredientId = ingredientId;
    }

    public BigDecimal getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(BigDecimal reservedQuantity) {
        if (reservedQuantity == null || reservedQuantity.signum() < 0) {
            throw new IllegalArgumentException("reservedQuantity must not be null or negative");
        }
        this.reservedQuantity = reservedQuantity;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        this.status = status;
    }

    @Override
    public String toString() {
        return "Reservation{" +
                "reservationId=" + reservationId +
                ", orderId=" + orderId +
                ", ingredientId=" + ingredientId +
                ", reservedQuantity=" + reservedQuantity +
                ", status=" + status +
                '}';
    }
}