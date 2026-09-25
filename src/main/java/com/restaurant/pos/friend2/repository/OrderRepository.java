package com.restaurant.pos.friend2.repository;

import com.restaurant.pos.friend2.config.DatabaseConnection;
import com.restaurant.pos.friend2.exception.DatabaseException;
import com.restaurant.pos.friend2.model.Order;
import com.restaurant.pos.friend2.model.OrderStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Plain JDBC DAO for the `orders` table. No Spring JdbcTemplate.
 * Methods that participate in a multi-step transaction accept the
 * caller's Connection so all operations share one transaction.
 */
public class OrderRepository {

    public Order save(Connection conn, Order order) {
        String sql = "INSERT INTO orders (customer_id, table_no, order_time, status, total_amount) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, order.getCustomerId());
            ps.setInt(2, order.getTableNo());
            ps.setTimestamp(3, Timestamp.valueOf(order.getOrderTime()));
            ps.setString(4, order.getStatus().name());
            ps.setBigDecimal(5, order.getTotalAmount());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    order.setOrderId(keys.getLong(1));
                }
            }
            return order;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save order", e);
        }
    }

    public Optional<Order> findById(Connection conn, Long orderId) {
        String sql = "SELECT * FROM orders WHERE order_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find order by id", e);
        }
    }

    public Optional<Order> findById(Long orderId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return findById(conn, orderId);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find order by id", e);
        }
    }

    /** Locks the order row (SELECT ... FOR UPDATE) within the caller's transaction. */
    public Optional<Order> findByIdForUpdate(Connection conn, Long orderId) {
        String sql = "SELECT * FROM orders WHERE order_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to lock order row", e);
        }
    }

    public void updateStatus(Connection conn, Long orderId, OrderStatus status) {
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, orderId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update order status", e);
        }
    }

    public void updateLockTime(Connection conn, Long orderId, LocalDateTime lockTime) {
        String sql = "UPDATE orders SET lock_time = ? WHERE order_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(lockTime));
            ps.setLong(2, orderId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update order lock_time", e);
        }
    }

    public void updateFinishTime(Connection conn, Long orderId, LocalDateTime finishTime) {
        String sql = "UPDATE orders SET finish_time = ? WHERE order_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(finishTime));
            ps.setLong(2, orderId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update order finish_time", e);
        }
    }

    public void updateTotalAmount(Connection conn, Long orderId, java.math.BigDecimal totalAmount) {
        String sql = "UPDATE orders SET total_amount = ? WHERE order_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, totalAmount);
            ps.setLong(2, orderId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update order total_amount", e);
        }
    }

    /** Finds all PENDING orders whose 10-minute window has already expired (for startup recovery). */
    public List<Order> findExpiredPendingOrders(Connection conn, LocalDateTime cutoff) {
        String sql = "SELECT * FROM orders WHERE status = 'PENDING' AND order_time <= ?";
        List<Order> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(cutoff));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query expired pending orders", e);
        }
    }

    /** Finds all PENDING orders whose window has NOT yet expired (for startup rescheduling). */
    public List<Order> findActivePendingOrders(Connection conn, LocalDateTime cutoff) {
        String sql = "SELECT * FROM orders WHERE status = 'PENDING' AND order_time > ?";
        List<Order> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(cutoff));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query active pending orders", e);
        }
    }

    private Order mapRow(ResultSet rs) throws SQLException {
        Timestamp lockTs = rs.getTimestamp("lock_time");
        Timestamp finishTs = rs.getTimestamp("finish_time");
        return new Order(
                rs.getLong("order_id"),
                rs.getLong("customer_id"),
                rs.getInt("table_no"),
                rs.getTimestamp("order_time").toLocalDateTime(),
                lockTs == null ? null : lockTs.toLocalDateTime(),
                finishTs == null ? null : finishTs.toLocalDateTime(),
                OrderStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("total_amount")
        );
    }
}