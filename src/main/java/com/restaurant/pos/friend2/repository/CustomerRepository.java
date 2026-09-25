package com.restaurant.pos.friend2.repository;

import com.restaurant.pos.friend2.config.DatabaseConnection;
import com.restaurant.pos.friend2.exception.DatabaseException;
import com.restaurant.pos.friend2.model.Customer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * Plain JDBC DAO for the `customers` table. No Spring JdbcTemplate.
 */
public class CustomerRepository {

    public Customer save(Customer customer) {
        String sql = "INSERT INTO customers (name, phone) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, customer.getName());
            ps.setString(2, customer.getPhone());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    customer.setCustomerId(keys.getLong(1));
                }
            }
            return customer;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save customer", e);
        }
    }

    /** Same as {@link #save(Customer)} but participates in the caller's transaction. */
    public Customer save(Connection conn, Customer customer) {
        String sql = "INSERT INTO customers (name, phone) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, customer.getName());
            ps.setString(2, customer.getPhone());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    customer.setCustomerId(keys.getLong(1));
                }
            }
            return customer;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save customer", e);
        }
    }

    public Optional<Customer> findById(Long customerId) {
        String sql = "SELECT customer_id, name, phone FROM customers WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find customer by id", e);
        }
    }

    public Optional<Customer> findByPhone(String phone) {
        String sql = "SELECT customer_id, name, phone FROM customers WHERE phone = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find customer by phone", e);
        }
    }

    private Customer mapRow(ResultSet rs) throws SQLException {
        return new Customer(
                rs.getLong("customer_id"),
                rs.getString("name"),
                rs.getString("phone")
        );
    }
}