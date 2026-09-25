package com.restaurant.pos.friend2.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Schedules the automatic 10-minute order lock using a
 * ScheduledExecutorService (never raw, uncontrolled Threads).
 *
 * The database (Order.orderTime) remains the single source of truth: this
 * scheduler is only an optimization to lock orders close to on-time.
 * OrderService independently re-verifies elapsed time from the DB before
 * actually locking, and {@link com.restaurant.pos.friend2.service.OrderService#recoverPendingOrdersOnStartup()}
 * recovers any tasks lost by an application restart.
 */
public class ReservationScheduler {

    private final ScheduledExecutorService executor =
            Executors.newScheduledThreadPool(4, runnable -> {
                Thread t = new Thread(runnable, "reservation-scheduler");
                t.setDaemon(true);
                return t;
            });

    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    private OrderService orderService;

    /** Set after construction from Main to avoid a circular constructor dependency. */
    public void setOrderService(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Schedules lockOrder(orderId) to run 10 minutes after orderTime.
     * If that time has already passed, the task runs almost immediately -
     * OrderService.lockOrder() will simply verify eligibility again.
     */
    public void scheduleAutoLock(Long orderId, LocalDateTime orderTime) {
        LocalDateTime unlockAt = orderTime.plusMinutes(OrderService.EDIT_WINDOW_MINUTES);
        long delayMillis = Duration.between(LocalDateTime.now(), unlockAt).toMillis();
        if (delayMillis < 0) {
            delayMillis = 0;
        }

        ScheduledFuture<?> future = executor.schedule(() -> {
            try {
                if (orderService != null) {
                    orderService.lockOrder(orderId);
                }
            } finally {
                scheduledTasks.remove(orderId);
            }
        }, delayMillis, TimeUnit.MILLISECONDS);

        scheduledTasks.put(orderId, future);
    }

    /** Cancels a scheduled lock task, e.g. because the order was cancelled. */
    public void cancelScheduledLock(Long orderId) {
        ScheduledFuture<?> future = scheduledTasks.remove(orderId);
        if (future != null) {
            future.cancel(false);
        }
    }

    /** Gracefully shuts down the executor, e.g. on application exit. */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}