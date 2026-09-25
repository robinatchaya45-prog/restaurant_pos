package com.restaurant.pos.friend2.service;

import com.restaurant.pos.friend2.exception.InsufficientStockException;
import com.restaurant.pos.friend2.exception.ReservationException;
import com.restaurant.pos.friend2.inventory.InventoryService;
import com.restaurant.pos.friend2.model.Reservation;
import com.restaurant.pos.friend2.model.ReservationStatus;
import com.restaurant.pos.friend2.repository.ReservationRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Handles the critical stock-reservation section.
 *
 * Two independent safety nets are combined, per the module spec:
 *   1. A JVM-level `synchronized` block, so that within THIS process no two
 *      threads even start racing for the same ingredient rows.
 *   2. Database-level pessimistic locking (SELECT ... FOR UPDATE) inside a
 *      JDBC transaction, so that correctness is guaranteed even across
 *      multiple application instances / processes - the `synchronized`
 *      keyword alone would NOT be enough for that.
 *
 * All methods here expect to run INSIDE a transaction that the caller
 * (OrderService) already opened on the given Connection; this class never
 * calls commit()/rollback() itself.
 */
public class ReservationService {

    private static final Object RESERVATION_LOCK = new Object();

    private final ReservationRepository reservationRepository;
    private final InventoryService inventoryService;

    public ReservationService(ReservationRepository reservationRepository,
                               InventoryService inventoryService) {
        this.reservationRepository = reservationRepository;
        this.inventoryService = inventoryService;
    }

    /**
     * Reserves stock for a brand-new order and inserts the corresponding
     * reservation rows. requiredIngredients maps ingredientId -> total
     * quantity required across the whole cart.
     *
     * Never reserves more than is currently available: every ingredient
     * row is locked (FOR UPDATE) and re-checked immediately before the
     * deduction happens.
     */
    public void reserveForNewOrder(Connection conn, Long orderId, Map<Long, BigDecimal> requiredIngredients) {
        if (requiredIngredients == null || requiredIngredients.isEmpty()) {
            return;
        }
        synchronized (RESERVATION_LOCK) {
            // Lock ingredient rows in a deterministic (sorted) order across
            // ALL callers to avoid circular-wait deadlocks when two orders
            // need overlapping ingredients.
            Map<Long, BigDecimal> sorted = new TreeMap<>(requiredIngredients);

            for (Map.Entry<Long, BigDecimal> entry : sorted.entrySet()) {
                Long ingredientId = entry.getKey();
                BigDecimal required = entry.getValue();

                inventoryService.lockIngredientRow(conn, ingredientId);
                BigDecimal available = inventoryService.getAvailableStock(conn, ingredientId);

                if (available.compareTo(required) < 0) {
                    throw new InsufficientStockException(
                            "Insufficient stock for ingredient " + ingredientId +
                                    ": required=" + required + ", available=" + available);
                }
            }

            // All checks passed: perform the deductions + insert reservations.
            for (Map.Entry<Long, BigDecimal> entry : sorted.entrySet()) {
                Long ingredientId = entry.getKey();
                BigDecimal required = entry.getValue();

                inventoryService.reserveIngredient(conn, ingredientId, required);

                Reservation reservation = new Reservation();
                reservation.setOrderId(orderId);
                reservation.setIngredientId(ingredientId);
                reservation.setReservedQuantity(required);
                reservation.setStatus(ReservationStatus.ACTIVE);
                reservationRepository.save(conn, reservation);
            }
        }
    }

    /**
     * Reserves ADDITIONAL stock on top of an existing order (quantity
     * increase during the 10-minute edit window). Existing ACTIVE
     * reservations for the same ingredient are topped up rather than
     * duplicated.
     */
    public void reserveAdditional(Connection conn, Long orderId, Map<Long, BigDecimal> additionalIngredients) {
        if (additionalIngredients == null || additionalIngredients.isEmpty()) {
            return;
        }
        synchronized (RESERVATION_LOCK) {
            Map<Long, BigDecimal> sorted = new TreeMap<>(additionalIngredients);

            for (Map.Entry<Long, BigDecimal> entry : sorted.entrySet()) {
                Long ingredientId = entry.getKey();
                BigDecimal additional = entry.getValue();

                inventoryService.lockIngredientRow(conn, ingredientId);
                BigDecimal available = inventoryService.getAvailableStock(conn, ingredientId);

                if (available.compareTo(additional) < 0) {
                    throw new InsufficientStockException(
                            "Insufficient stock to increase quantity for ingredient " + ingredientId +
                                    ": required additional=" + additional + ", available=" + available);
                }
            }

            for (Map.Entry<Long, BigDecimal> entry : sorted.entrySet()) {
                Long ingredientId = entry.getKey();
                BigDecimal additional = entry.getValue();

                inventoryService.reserveIngredient(conn, ingredientId, additional);

                List<Reservation> existing =
                        reservationRepository.findActiveByOrderIdAndIngredientId(conn, orderId, ingredientId);

                if (existing.isEmpty()) {
                    Reservation reservation = new Reservation();
                    reservation.setOrderId(orderId);
                    reservation.setIngredientId(ingredientId);
                    reservation.setReservedQuantity(additional);
                    reservation.setStatus(ReservationStatus.ACTIVE);
                    reservationRepository.save(conn, reservation);
                } else {
                    Reservation reservation = existing.get(0);
                    BigDecimal newQty = reservation.getReservedQuantity().add(additional);
                    reservationRepository.updateQuantity(conn, reservation.getReservationId(), newQty);
                }
            }
        }
    }

    /**
     * Releases part of an existing reservation back to available stock
     * (quantity decrease during the 10-minute edit window).
     */
    public void releasePartial(Connection conn, Long orderId, Map<Long, BigDecimal> releasedIngredients) {
        if (releasedIngredients == null || releasedIngredients.isEmpty()) {
            return;
        }
        synchronized (RESERVATION_LOCK) {
            Map<Long, BigDecimal> sorted = new TreeMap<>(releasedIngredients);

            for (Map.Entry<Long, BigDecimal> entry : sorted.entrySet()) {
                Long ingredientId = entry.getKey();
                BigDecimal releasedQty = entry.getValue();

                inventoryService.lockIngredientRow(conn, ingredientId);

                List<Reservation> existing =
                        reservationRepository.findActiveByOrderIdAndIngredientId(conn, orderId, ingredientId);

                if (existing.isEmpty()) {
                    throw new ReservationException(
                            "No active reservation found for order " + orderId + " ingredient " + ingredientId);
                }

                Reservation reservation = existing.get(0);
                BigDecimal newQty = reservation.getReservedQuantity().subtract(releasedQty);
                if (newQty.signum() < 0) {
                    newQty = BigDecimal.ZERO;
                }

                inventoryService.releaseIngredient(conn, ingredientId, releasedQty);

                if (newQty.signum() == 0) {
                    reservationRepository.updateStatus(conn, reservation.getReservationId(), ReservationStatus.RELEASED);
                } else {
                    reservationRepository.updateQuantity(conn, reservation.getReservationId(), newQty);
                }
            }
        }
    }

    /**
     * Releases ALL active reservations for a cancelled order back to
     * available stock and marks them RELEASED.
     */
    public void releaseAllForOrder(Connection conn, Long orderId) {
        synchronized (RESERVATION_LOCK) {
            List<Reservation> activeReservations = reservationRepository.findActiveByOrderId(conn, orderId);
            for (Reservation reservation : activeReservations) {
                inventoryService.lockIngredientRow(conn, reservation.getIngredientId());
                inventoryService.releaseIngredient(conn, reservation.getIngredientId(), reservation.getReservedQuantity());
                reservationRepository.updateStatus(conn, reservation.getReservationId(), ReservationStatus.RELEASED);
            }
        }
    }

    /**
     * Confirms ALL active reservations for an order (used when the
     * 10-minute window naturally expires and the order gets LOCKED).
     */
    public void confirmAllForOrder(Connection conn, Long orderId) {
        synchronized (RESERVATION_LOCK) {
            reservationRepository.confirmAllActiveForOrder(conn, orderId);
        }
    }

    public List<Reservation> findActiveByOrderId(Connection conn, Long orderId) {
        return reservationRepository.findActiveByOrderId(conn, orderId);
    }
}