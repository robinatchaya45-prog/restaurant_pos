package com.restaurant.pos.friend2.inventory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Map;

/**
 * API contract between the Order/Reservation module (Friend 2) and the
 * Inventory module (Friend 1). Friend 2 depends ONLY on this interface,
 * never on Friend 1's Ingredient / MenuItem / Recipe classes, keeping the
 * two modules loosely coupled.
 *
 * All methods that participate in a reservation transaction accept the
 * caller's JDBC Connection so that stock checks/updates happen on the
 * SAME connection/transaction as the order creation (required for
 * SELECT ... FOR UPDATE row locking to be effective).
 *
 * Friend 1 is expected to provide the real implementation of this
 * interface, backed by the `ingredients` / `menu_items` / `recipes`
 * tables. Until that implementation is wired in, {@link InventoryServiceStub}
 * provides an in-memory stand-in so this module remains independently
 * compilable and testable.
 */
public interface InventoryService {

    /**
     * Calculates how much of each ingredient is required to prepare
     * {@code quantity} units of the given menu item.
     *
     * @param itemId   Friend 1's MenuItem primary key
     * @param quantity number of units of that menu item
     * @return map of ingredientId -> required quantity
     */
    Map<Long, BigDecimal> calculateRequiredIngredients(Long itemId, int quantity);

    /**
     * Reads the currently available stock quantity for an ingredient.
     * Should be called AFTER {@link #lockIngredientRow(Connection, Long)}
     * within the same transaction to get an accurate, race-free value.
     */
    BigDecimal getAvailableStock(Connection connection, Long ingredientId);

    /**
     * Locks the ingredient's row using SELECT ... FOR UPDATE so concurrent
     * transactions cannot read/modify the same row until this transaction
     * commits or rolls back.
     */
    void lockIngredientRow(Connection connection, Long ingredientId);

    /**
     * Deducts {@code quantity} from the ingredient's available stock as
     * part of creating a reservation. Must be called on a connection that
     * already holds the row lock for this ingredient.
     */
    void reserveIngredient(Connection connection, Long ingredientId, BigDecimal quantity);

    /**
     * Returns {@code quantity} back to the ingredient's available stock,
     * e.g. when a reservation is released due to cancellation or a
     * quantity decrease.
     */
    void releaseIngredient(Connection connection, Long ingredientId, BigDecimal quantity);
}