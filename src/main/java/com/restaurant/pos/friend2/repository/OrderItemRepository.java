package com.restaurant.pos.friend2.repository;

import com.restaurant.pos.friend2.exception.DatabaseException;
import com.restaurant.pos.friend2.model.OrderItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Plain JDBC DAO for the `order_items` table. No Spring JdbcTemplate.
 */
public class OrderItemRepository {

    public OrderItem save(Connection conn, OrderItem item) {
        String sql = "INSERT INTO order_items (order_id, item_id, quantity, price) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, item.getOrderId());
            ps.setLong(2, item.getItemId());
            ps.setInt(3, item.getQuantity());
            ps.setBigDecimal(4, item.getPrice());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    item.setId(keys.getLong(1));
                }
            }
            return item;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save order item", e);
        }
    }

    public List<OrderItem> findByOrderId(Connection conn, Long orderId) {
        String sql = "SELECT * FROM order_items WHERE order_id = ?";
        List<OrderItem> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find order items by order id", e);
        }
    }

    public Optional<OrderItem> findByOrderIdAndItemId(Connection conn, Long orderId, Long itemId) {
        String sql = "SELECT * FROM order_items WHERE order_id = ? AND item_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            ps.setLong(2, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find order item", e);
        }
    }

    public void updateQuantity(Connection conn, Long orderItemId, Integer newQuantity) {
        String sql = "UPDATE order_items SET quantity = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newQuantity);
            ps.setLong(2, orderItemId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update order item quantity", e);
        }
    }

    public void deleteById(Connection conn, Long orderItemId) {
        String sql = "DELETE FROM order_items WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderItemId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete order item", e);
        }
    }

    private OrderItem mapRow(ResultSet rs) throws SQLException {
        return new OrderItem(
                rs.getLong("id"),
                rs.getLong("order_id"),
                rs.getLong("item_id"),
                rs.getInt("quantity"),
                rs.getBigDecimal("price")
        );
    }
}