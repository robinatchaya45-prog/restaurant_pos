package com.restaurant.pos.friend2.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Plain domain model representing a customer Order.
 * No SQL / persistence logic belongs here (see OrderRepository).
 */
public class Order {

    private Long orderId;
    private Long customerId;
    private Integer tableNo;
    private LocalDateTime orderTime;
    private LocalDateTime lockTime;
    private LocalDateTime finishTime;
    private OrderStatus status;
    private BigDecimal totalAmount;

    public Order() {
        this.status = OrderStatus.PENDING;
        this.totalAmount = BigDecimal.ZERO;
    }

    public Order(Long orderId, Long customerId, Integer tableNo, LocalDateTime orderTime,
                 LocalDateTime lockTime, LocalDateTime finishTime, OrderStatus status,
                 BigDecimal totalAmount) {
        this.orderId = orderId;
        this.customerId = customerId;
        setTableNo(tableNo);
        this.orderTime = orderTime;
        this.lockTime = lockTime;
        this.finishTime = finishTime;
        this.status = status == null ? OrderStatus.PENDING : status;
        this.totalAmount = totalAmount == null ? BigDecimal.ZERO : totalAmount;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        if (customerId == null) {
            throw new IllegalArgumentException("customerId is mandatory");
        }
        this.customerId = customerId;
    }

    public Integer getTableNo() {
        return tableNo;
    }

    /**
     * tableNo is mandatory: the customer must enter a table number
     * before an order can be placed.
     */
    public void setTableNo(Integer tableNo) {
        if (tableNo == null) {
            throw new IllegalArgumentException("tableNo is mandatory");
        }
        if (tableNo <= 0) {
            throw new IllegalArgumentException("tableNo must be a positive number");
        }
        this.tableNo = tableNo;
    }

    public LocalDateTime getOrderTime() {
        return orderTime;
    }

    public void setOrderTime(LocalDateTime orderTime) {
        this.orderTime = orderTime;
    }

    public LocalDateTime getLockTime() {
        return lockTime;
    }

    public void setLockTime(LocalDateTime lockTime) {
        this.lockTime = lockTime;
    }

    public LocalDateTime getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(LocalDateTime finishTime) {
        this.finishTime = finishTime;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        if (totalAmount == null || totalAmount.signum() < 0) {
            throw new IllegalArgumentException("totalAmount must not be null or negative");
        }
        this.totalAmount = totalAmount;
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderId=" + orderId +
                ", customerId=" + customerId +
                ", tableNo=" + tableNo +
                ", orderTime=" + orderTime +
                ", lockTime=" + lockTime +
                ", finishTime=" + finishTime +
                ", status=" + status +
                ", totalAmount=" + totalAmount +
                '}';
    }
}