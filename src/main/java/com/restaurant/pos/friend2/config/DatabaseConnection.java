package com.restaurant.pos.friend2.config;

import com.restaurant.pos.friend2.exception.DatabaseException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Centralized JDBC connection factory. No Spring DataSource is used.
 *
 * Configuration can be overridden with environment variables / -D system
 * properties so the same shared repository works on every machine:
 *
 *   DB_URL      (default: jdbc:mysql://localhost:3306/restaurant_pos)
 *   DB_USER     (default: root)
 *   DB_PASSWORD (default: empty string)
 */
public final class DatabaseConnection {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/restaurant_pos?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private static final String URL = resolve("DB_URL", DEFAULT_URL);
    private static final String USER = resolve("DB_USER", DEFAULT_USER);
    private static final String PASSWORD = resolve("DB_PASSWORD", DEFAULT_PASSWORD);

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new DatabaseException("MySQL JDBC Driver not found on classpath", e);
        }
    }

    private DatabaseConnection() {
        // utility class, no instances
    }

    private static String resolve(String key, String defaultValue) {
        String sys = System.getProperty(key);
        if (sys != null && !sys.isBlank()) {
            return sys;
        }
        String env = System.getenv(key);
        if (env != null && !env.isBlank()) {
            return env;
        }
        return defaultValue;
    }

    /**
     * Returns a brand-new JDBC connection. Callers are responsible for
     * closing the connection (try-with-resources is recommended) and,
     * for multi-step operations, for managing the transaction boundary
     * (setAutoCommit/commit/rollback) on the SAME connection instance.
     */
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            throw new DatabaseException("Unable to obtain a database connection", e);
        }
    }
}