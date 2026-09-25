package com.restaurant.pos.friend2.swing;

import java.math.BigDecimal;

/**
 * Lightweight, read-only view of a menu item for display purposes only.
 *
 * This module does NOT own MenuItem.java (that belongs to Friend 1's
 * inventory/menu module) and must not duplicate it. In the fully
 * integrated system, instances of this class would be populated from
 * Friend 1's menu service/API contract instead of the demo data used
 * in {@link CustomerMenuFrame#loadDemoMenu()}.
 */
public class MenuItemView {

    private final Long itemId;
    private final String name;
    private final BigDecimal price;
    private final boolean available;

    public MenuItemView(Long itemId, String name, BigDecimal price, boolean available) {
        this.itemId = itemId;
        this.name = name;
        this.price = price;
        this.available = available;
    }

    public Long getItemId() {
        return itemId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public boolean isAvailable() {
        return available;
    }

    @Override
    public String toString() {
        return name + " - $" + price + (available ? "" : " (unavailable)");
    }
}