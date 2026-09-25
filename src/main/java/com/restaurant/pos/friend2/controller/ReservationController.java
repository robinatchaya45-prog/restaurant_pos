package com.restaurant.pos.friend2.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.restaurant.pos.friend2.config.DatabaseConnection;
import com.restaurant.pos.friend2.exception.DatabaseException;
import com.restaurant.pos.friend2.model.Reservation;
import com.restaurant.pos.friend2.service.ReservationService;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Routes HTTP requests under /api/reservations to the ReservationService.
 * Contains NO business logic and NO SQL - both live in lower layers.
 *
 * Handled routes:
 *   GET /api/reservations/order/{orderId} -> list active reservations for an order
 */
public class ReservationController implements HttpHandler {

    private static final String BASE_PATH = "/api/reservations/order/";

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if (!"GET".equalsIgnoreCase(method) || !path.startsWith(BASE_PATH)) {
            HttpUtils.sendError(exchange, 405, "Method not allowed");
            return;
        }

        Long orderId = HttpUtils.extractIdSegment(path, BASE_PATH);
        if (orderId == null) {
            HttpUtils.sendError(exchange, 400, "Invalid order id");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            List<Reservation> reservations = reservationService.findActiveByOrderId(conn, orderId);
            HttpUtils.sendJson(exchange, 200, reservations);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load reservations", e);
        }
    }
}