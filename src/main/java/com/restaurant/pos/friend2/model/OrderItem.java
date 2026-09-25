package com.restaurant.pos.friend2.model;

import java.math.BigDecimal;

/**
 * Plain domain model representing a single line item of an Order.
 * itemId references Friend 1's MenuItem primary key, but this class
 * does NOT depend on Friend 1's MenuItem class (loose coupling).
 */
public class OrderItem {

    private Long id;
    private Long orderId;
    private Long itemId;
    private Integer quantity;
    private BigDecimal price;

    public OrderItem() {
    }

    public OrderItem(Long id, Long orderId, Long itemId, Integer quantity, BigDecimal price) {
        this.id = id;
        this.orderId = orderId;
        setItemId(itemId);
        setQuantity(quantity);
        setPrice(price);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        if (itemId == null) {
            throw new IllegalArgumentException("itemId must not be null");
        }
        this.itemId = itemId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("quantity must be a positive number");
        }
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException("price must not be null or negative");
        }
        this.price = price;
    }

    public BigDecimal getSubtotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    @Override
    public String toString() {
        return "OrderItem{" +
                "id=" + id +
                ", orderId=" + orderId +
                ", itemId=" + itemId +
                ", quantity=" + quantity +
                ", price=" + price +
                '}';
    }
}