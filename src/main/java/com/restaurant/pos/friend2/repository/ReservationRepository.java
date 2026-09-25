package com.restaurant.pos.friend2.repository;

import com.restaurant.pos.friend2.exception.DatabaseException;
import com.restaurant.pos.friend2.model.Reservation;
import com.restaurant.pos.friend2.model.ReservationStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Plain JDBC DAO for the `order_reservations` table. No Spring JdbcTemplate.
 */
public class ReservationRepository {

    public Reservation save(Connection conn, Reservation reservation) {
        String sql = "INSERT INTO order_reservations (order_id, ingredient_id, reserved_quantity, status) " +
                "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, reservation.getOrderId());
            ps.setLong(2, reservation.getIngredientId());
            ps.setBigDecimal(3, reservation.getReservedQuantity());
            ps.setString(4, reservation.getStatus().name());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    reservation.setReservationId(keys.getLong(1));
                }
            }
            return reservation;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save reservation", e);
        }
    }

    public List<Reservation> findByOrderId(Connection conn, Long orderId) {
        String sql = "SELECT * FROM order_reservations WHERE order_id = ?";
        List<Reservation> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find reservations by order id", e);
        }
    }

    public List<Reservation> findActiveByOrderId(Connection conn, Long orderId) {
        String sql = "SELECT * FROM order_reservations WHERE order_id = ? AND status = 'ACTIVE'";
        List<Reservation> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find active reservations by order id", e);
        }
    }

    public List<Reservation> findActiveByOrderIdAndIngredientId(Connection conn, Long orderId, Long ingredientId) {
        String sql = "SELECT * FROM order_reservations WHERE order_id = ? AND ingredient_id = ? AND status = 'ACTIVE'";
        List<Reservation> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            ps.setLong(2, ingredientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find active reservation", e);
        }
    }

    public void updateStatus(Connection conn, Long reservationId, ReservationStatus status) {
        String sql = "UPDATE order_reservations SET status = ? WHERE reservation_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, reservationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update reservation status", e);
        }
    }

    public void updateQuantity(Connection conn, Long reservationId, java.math.BigDecimal newQuantity) {
        String sql = "UPDATE order_reservations SET reserved_quantity = ? WHERE reservation_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newQuantity);
            ps.setLong(2, reservationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update reservation quantity", e);
        }
    }

    /** Marks every ACTIVE reservation for the given order as CONFIRMED (used by the auto-lock job). */
    public void confirmAllActiveForOrder(Connection conn, Long orderId) {
        String sql = "UPDATE order_reservations SET status = 'CONFIRMED' WHERE order_id = ? AND status = 'ACTIVE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to confirm reservations for order", e);
        }
    }

    private Reservation mapRow(ResultSet rs) throws SQLException {
        return new Reservation(
                rs.getLong("reservation_id"),
                rs.getLong("order_id"),
                rs.getLong("ingredient_id"),
                rs.getBigDecimal("reserved_quantity"),
                ReservationStatus.valueOf(rs.getString("status"))
        );
    }
}