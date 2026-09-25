package com.restaurant.pos.friend2.service;

import com.restaurant.pos.friend2.config.DatabaseConnection;
import com.restaurant.pos.friend2.exception.DatabaseException;
import com.restaurant.pos.friend2.exception.InvalidQuantityException;
import com.restaurant.pos.friend2.exception.InvalidTableNumberException;
import com.restaurant.pos.friend2.exception.OrderAlreadyCancelledException;
import com.restaurant.pos.friend2.exception.OrderNotEditableException;
import com.restaurant.pos.friend2.inventory.InventoryService;
import com.restaurant.pos.friend2.model.Order;
import com.restaurant.pos.friend2.model.OrderItem;
import com.restaurant.pos.friend2.model.OrderStatus;
import com.restaurant.pos.friend2.repository.OrderItemRepository;
import com.restaurant.pos.friend2.repository.OrderRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Business logic for placing, editing, cancelling and locking orders.
 * Swing and the HTTP handlers must call this layer only; they must never
 * open a Connection or run SQL themselves.
 */
public class OrderService {

    public static final long EDIT_WINDOW_MINUTES = 10;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReservationService reservationService;
    private final InventoryService inventoryService;

    /** Set after construction from Main to avoid a circular constructor dependency. */
    private ReservationScheduler reservationScheduler;

    public OrderService(OrderRepository orderRepository,
                         OrderItemRepository orderItemRepository,
                         ReservationService reservationService,
                         InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.reservationService = reservationService;
        this.inventoryService = inventoryService;
    }

    public void setReservationScheduler(ReservationScheduler reservationScheduler) {
        this.reservationScheduler = reservationScheduler;
    }

    /** A single requested line item, as submitted from the cart. */
    public static class CartLine {
        public final Long itemId;
        public final Integer quantity;
        public final BigDecimal price;

        public CartLine(Long itemId, Integer quantity, BigDecimal price) {
            this.itemId = itemId;
            this.quantity = quantity;
            this.price = price;
        }
    }

    /**
     * Places a brand-new order for a customer, table number and cart.
     * Runs the full flow described in the module spec inside ONE JDBC
     * transaction: validate -> calculate required ingredients -> lock ->
     * re-check stock -> reserve -> create order -> create items ->
     * create reservations -> commit. Rolls back entirely on any failure.
     */
    public Order placeOrder(Long customerId, Integer tableNo, List<CartLine> cartLines) {
        validateTableNumber(tableNo);
        validateCart(cartLines);

        Connection conn = DatabaseConnection.getConnection();
        try {
            conn.setAutoCommit(false);

            // 1-3: validation already done above (customer existence is the
            // caller's responsibility via CustomerService before invoking this).

            // 4. Calculate required ingredients for the whole cart.
            Map<Long, BigDecimal> requiredIngredients = new HashMap<>();
            for (CartLine line : cartLines) {
                Map<Long, BigDecimal> perItem =
                        inventoryService.calculateRequiredIngredients(line.itemId, line.quantity);
                for (Map.Entry<Long, BigDecimal> entry : perItem.entrySet()) {
                    requiredIngredients.merge(entry.getKey(), entry.getValue(), BigDecimal::add);
                }
            }

            // 5-8. Lock rows, re-check stock, reserve (handled atomically
            // inside ReservationService using a synchronized + FOR UPDATE section).
            BigDecimal total = BigDecimal.ZERO;
            for (CartLine line : cartLines) {
                total = total.add(line.price.multiply(BigDecimal.valueOf(line.quantity)));
            }

            Order order = new Order();
            order.setCustomerId(customerId);
            order.setTableNo(tableNo);
            order.setOrderTime(LocalDateTime.now());
            order.setStatus(OrderStatus.PENDING);
            order.setTotalAmount(total);

            // 9. Create order first so reservations can reference its id.
            orderRepository.save(conn, order);

            // 10. Create order items.
            for (CartLine line : cartLines) {
                OrderItem item = new OrderItem(null, order.getOrderId(), line.itemId, line.quantity, line.price);
                orderItemRepository.save(conn, item);
            }

            // 11. Create reservation records (reserves stock, throws if insufficient).
            reservationService.reserveForNewOrder(conn, order.getOrderId(), requiredIngredients);

            // 12. Commit transaction.
            conn.commit();

            // Schedule the automatic 10-minute lock (outside the transaction).
            if (reservationScheduler != null) {
                reservationScheduler.scheduleAutoLock(order.getOrderId(), order.getOrderTime());
            }

            return order;
        } catch (RuntimeException e) {
            rollbackQuietly(conn);
            throw e;
        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new DatabaseException("Failed to place order", e);
        } finally {
            closeQuietly(conn);
        }
    }

    /**
     * Cancels an order within the 10-minute edit window: releases all
     * active reservations back to stock and marks the order CANCELLED.
     */
    public void cancelOrder(Long orderId) {
        Connection conn = DatabaseConnection.getConnection();
        try {
            conn.setAutoCommit(false);

            Order order = orderRepository.findByIdForUpdate(conn, orderId)
                    .orElseThrow(() -> new DatabaseException("Order not found: " + orderId));

            if (order.getStatus() == OrderStatus.CANCELLED) {
                throw new OrderAlreadyCancelledException("Order " + orderId + " is already cancelled");
            }
            if (order.getStatus() != OrderStatus.PENDING || isEditWindowExpired(order)) {
                throw new OrderNotEditableException(
                        "Order " + orderId + " can no longer be cancelled: the 10-minute window has expired");
            }

            // 1-3. Release ACTIVE reservations, mark RELEASED.
            reservationService.releaseAllForOrder(conn, orderId);

            // 4. Mark order CANCELLED.
            orderRepository.updateStatus(conn, orderId, OrderStatus.CANCELLED);
            orderRepository.updateFinishTime(conn, orderId, LocalDateTime.now());

            // 5. Commit.
            conn.commit();

            if (reservationScheduler != null) {
                reservationScheduler.cancelScheduledLock(orderId);
            }
        } catch (RuntimeException e) {
            rollbackQuietly(conn);
            throw e;
        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new DatabaseException("Failed to cancel order", e);
        } finally {
            closeQuietly(conn);
        }
    }

    /**
     * Increases the quantity of an existing order item, reserving the
     * extra ingredients required. Rolls back with InsufficientStockException
     * if there isn't enough stock.
     */
    public void increaseItemQuantity(Long orderId, Long itemId, int additionalQuantity) {
        if (additionalQuantity <= 0) {
            throw new InvalidQuantityException("additionalQuantity must be positive");
        }

        Connection conn = DatabaseConnection.getConnection();
        try {
            conn.setAutoCommit(false);

            Order order = requireEditableOrder(conn, orderId);

            OrderItem item = orderItemRepository.findByOrderIdAndItemId(conn, orderId, itemId)
                    .orElseThrow(() -> new DatabaseException("Order item not found for item " + itemId));

            // 1. Calculate additional ingredient requirement.
            Map<Long, BigDecimal> additionalIngredients =
                    inventoryService.calculateRequiredIngredients(itemId, additionalQuantity);

            // 2-4. Lock rows, check stock, reserve additional stock (throws if insufficient).
            reservationService.reserveAdditional(conn, orderId, additionalIngredients);

            // 5. Update order item quantity and order total.
            int newQuantity = item.getQuantity() + additionalQuantity;
            orderItemRepository.updateQuantity(conn, item.getId(), newQuantity);

            BigDecimal additionalCost = item.getPrice().multiply(BigDecimal.valueOf(additionalQuantity));
            orderRepository.updateTotalAmount(conn, orderId, order.getTotalAmount().add(additionalCost));

            // 6. Commit.
            conn.commit();
        } catch (RuntimeException e) {
            rollbackQuietly(conn);
            throw e;
        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new DatabaseException("Failed to increase item quantity", e);
        } finally {
            closeQuietly(conn);
        }
    }

    /**
     * Decreases the quantity of an existing order item, releasing the
     * corresponding reserved ingredients back to available stock.
     */
    public void decreaseItemQuantity(Long orderId, Long itemId, int reduceBy) {
        if (reduceBy <= 0) {
            throw new InvalidQuantityException("reduceBy must be positive");
        }

        Connection conn = DatabaseConnection.getConnection();
        try {
            conn.setAutoCommit(false);

            Order order = requireEditableOrder(conn, orderId);

            OrderItem item = orderItemRepository.findByOrderIdAndItemId(conn, orderId, itemId)
                    .orElseThrow(() -> new DatabaseException("Order item not found for item " + itemId));

            if (reduceBy >= item.getQuantity()) {
                throw new InvalidQuantityException(
                        "reduceBy must be smaller than the current quantity; remove the item to cancel it entirely");
            }

            // 1. Calculate released ingredient quantity.
            Map<Long, BigDecimal> releasedIngredients =
                    inventoryService.calculateRequiredIngredients(itemId, reduceBy);

            // 2-3. Return released quantity to available stock, update reservation.
            reservationService.releasePartial(conn, orderId, releasedIngredients);

            // 4. Update order item quantity and order total.
            int newQuantity = item.getQuantity() - reduceBy;
            orderItemRepository.updateQuantity(conn, item.getId(), newQuantity);

            BigDecimal releasedCost = item.getPrice().multiply(BigDecimal.valueOf(reduceBy));
            orderRepository.updateTotalAmount(conn, orderId, order.getTotalAmount().subtract(releasedCost));

            // 5. Commit.
            conn.commit();
        } catch (RuntimeException e) {
            rollbackQuietly(conn);
            throw e;
        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new DatabaseException("Failed to decrease item quantity", e);
        } finally {
            closeQuietly(conn);
        }
    }

    /**
     * Automatically locks an order once its 10-minute window has elapsed.
     * Called by ReservationScheduler - the customer never presses a lock
     * button. The database order_time is treated as the source of truth,
     * so the elapsed time is re-verified here rather than trusted blindly.
     */
    public void lockOrder(Long orderId) {
        Connection conn = DatabaseConnection.getConnection();
        try {
            conn.setAutoCommit(false);

            Optional<Order> maybeOrder = orderRepository.findByIdForUpdate(conn, orderId);
            if (maybeOrder.isEmpty()) {
                conn.rollback();
                return;
            }
            Order order = maybeOrder.get();

            // 2. Check current order status - only PENDING orders get locked.
            if (order.getStatus() != OrderStatus.PENDING) {
                conn.rollback();
                return;
            }

            // 3. Check whether 10 minutes have actually elapsed (DB time is authoritative).
            if (!isEditWindowExpired(order)) {
                conn.rollback();
                return;
            }

            // 4-5. Confirm ACTIVE reservations.
            reservationService.confirmAllForOrder(conn, orderId);

            // 6-7. Update order status to LOCKED and set lock_time.
            LocalDateTime lockTime = LocalDateTime.now();
            orderRepository.updateStatus(conn, orderId, OrderStatus.LOCKED);
            orderRepository.updateLockTime(conn, orderId, lockTime);

            // 8. Commit.
            conn.commit();
        } catch (RuntimeException e) {
            rollbackQuietly(conn);
            throw e;
        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new DatabaseException("Failed to lock order", e);
        } finally {
            closeQuietly(conn);
        }
    }

    /**
     * Startup recovery: finds PENDING orders whose 10-minute window has
     * already expired (because the app was restarted and in-memory
     * scheduled tasks were lost) and locks them immediately. Also
     * reschedules the remaining active PENDING orders.
     */
    public void recoverPendingOrdersOnStartup() {
        Connection conn = DatabaseConnection.getConnection();
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(EDIT_WINDOW_MINUTES);

            List<Order> expired = orderRepository.findExpiredPendingOrders(conn, cutoff);
            for (Order order : expired) {
                lockOrder(order.getOrderId());
            }

            List<Order> active = orderRepository.findActivePendingOrders(conn, cutoff);
            if (reservationScheduler != null) {
                for (Order order : active) {
                    reservationScheduler.scheduleAutoLock(order.getOrderId(), order.getOrderTime());
                }
            }
        } finally {
            closeQuietly(conn);
        }
    }

    public Optional<Order> findById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    public List<OrderItem> findItems(Long orderId) {
        Connection conn = DatabaseConnection.getConnection();
        try {
            return orderItemRepository.findByOrderId(conn, orderId);
        } finally {
            closeQuietly(conn);
        }
    }

    public long remainingEditSeconds(Order order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            return 0;
        }
        LocalDateTime expiry = order.getOrderTime().plusMinutes(EDIT_WINDOW_MINUTES);
        long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), expiry);
        return Math.max(seconds, 0);
    }

    // ---------------------------------------------------------------
    // internal helpers
    // ---------------------------------------------------------------

    private Order requireEditableOrder(Connection conn, Long orderId) {
        Order order = orderRepository.findByIdForUpdate(conn, orderId)
                .orElseThrow(() -> new DatabaseException("Order not found: " + orderId));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new OrderAlreadyCancelledException("Order " + orderId + " is already cancelled");
        }
        if (order.getStatus() != OrderStatus.PENDING || isEditWindowExpired(order)) {
            throw new OrderNotEditableException(
                    "Order " + orderId + " can no longer be edited: the 10-minute window has expired");
        }
        return order;
    }

    private boolean isEditWindowExpired(Order order) {
        LocalDateTime expiry = order.getOrderTime().plusMinutes(EDIT_WINDOW_MINUTES);
        return LocalDateTime.now().isAfter(expiry);
    }

    private void validateTableNumber(Integer tableNo) {
        if (tableNo == null) {
            throw new InvalidTableNumberException("Table number must not be empty");
        }
        if (tableNo <= 0) {
            throw new InvalidTableNumberException("Table number must be a positive number");
        }
    }

    private void validateCart(List<CartLine> cartLines) {
        if (cartLines == null || cartLines.isEmpty()) {
            throw new InvalidQuantityException("Cart must contain at least one item");
        }
        for (CartLine line : cartLines) {
            if (line.quantity == null || line.quantity <= 0) {
                throw new InvalidQuantityException("Item " + line.itemId + " has an invalid quantity");
            }
            if (line.price == null || line.price.signum() < 0) {
                throw new InvalidQuantityException("Item " + line.itemId + " has an invalid price");
            }
        }
    }

    private void rollbackQuietly(Connection conn) {
        try {
            if (conn != null) {
                conn.rollback();
            }
        } catch (SQLException ignored) {
            // best-effort rollback
        }
    }

    private void closeQuietly(Connection conn) {
        try {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        } catch (SQLException ignored) {
            // best-effort close
        }
    }
}