package com.restaurant.pos.friend2.inventory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.restaurant.pos.friend2.exception.DatabaseException;
import com.restaurant.pos.friend2.exception.InsufficientStockException;

/**
 * TEMPORARY stand-in implementation of {@link InventoryService}.
 *
 * This class exists ONLY so that the Friend 2 module (Order + Reservation)
 * is independently compilable and runnable before Friend 1's real
 * inventory implementation is merged into the shared repository.
 *
 * It assumes a simple `ingredients(ingredient_id BIGINT, available_stock DECIMAL)`
 * table exists (see resources/database.sql sample data section) and a
 * fixed 1:1 recipe of "1 unit of menu item consumes 1 unit of ingredient
 * with the same id" purely as a placeholder ratio.
 *
 * Replace this class with Friend 1's real implementation by swapping the
 * InventoryService instance injected into ReservationService/OrderService
 * in Main.java. No other class needs to change.
 */
public class InventoryServiceStub implements InventoryService {

    // Placeholder recipe ratio: itemId -> (ingredientId -> qty per unit)
    private final Map<Long, Map<Long, BigDecimal>> placeholderRecipes = new ConcurrentHashMap<>();

    @Override
    public Map<Long, BigDecimal> calculateRequiredIngredients(Long itemId, int quantity) {
        Map<Long, BigDecimal> perUnit = placeholderRecipes.getOrDefault(itemId, defaultRecipe(itemId));
        Map<Long, BigDecimal> result = new HashMap<>();
        for (Map.Entry<Long, BigDecimal> entry : perUnit.entrySet()) {
            result.put(entry.getKey(), entry.getValue().multiply(BigDecimal.valueOf(quantity)));
        }
        return result;
    }

    private Map<Long, BigDecimal> defaultRecipe(Long itemId) {
        // Placeholder: assumes ingredient id == item id, 1 unit per item.
        Map<Long, BigDecimal> map = new HashMap<>();
        map.put(itemId, BigDecimal.ONE);
        return map;
    }

    @Override
    public BigDecimal getAvailableStock(Connection connection, Long ingredientId) {
        String sql = "SELECT available_stock FROM ingredients WHERE ingredient_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, ingredientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("available_stock");
                }
                throw new InsufficientStockException("Ingredient not found: " + ingredientId);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to read ingredient stock", e);
        }
    }

    @Override
    public void lockIngredientRow(Connection connection, Long ingredientId) {
        String sql = "SELECT ingredient_id FROM ingredients WHERE ingredient_id = ? FOR UPDATE";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, ingredientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new InsufficientStockException("Ingredient not found: " + ingredientId);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to lock ingredient row", e);
        }
    }

    @Override
    public void reserveIngredient(Connection connection, Long ingredientId, BigDecimal quantity) {
        String sql = "UPDATE ingredients SET available_stock = available_stock - ? WHERE ingredient_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBigDecimal(1, quantity);
            ps.setLong(2, ingredientId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to reserve ingredient stock", e);
        }
    }

    @Override
    public void releaseIngredient(Connection connection, Long ingredientId, BigDecimal quantity) {
        String sql = "UPDATE ingredients SET available_stock = available_stock + ? WHERE ingredient_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBigDecimal(1, quantity);
            ps.setLong(2, ingredientId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to release ingredient stock", e);
        }
    }
}