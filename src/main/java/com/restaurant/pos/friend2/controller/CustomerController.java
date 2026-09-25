package com.restaurant.pos.friend2.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.restaurant.pos.friend2.model.Customer;
import com.restaurant.pos.friend2.service.CustomerService;

import java.io.IOException;

/**
 * Routes HTTP requests for /api/customers to the CustomerService.
 * Contains NO business logic and NO SQL - both live in lower layers.
 */
public class CustomerController implements HttpHandler {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /** Request/response DTO kept private to this handler. */
    private static class CustomerRequest {
        String name;
        String phone;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            if ("POST".equalsIgnoreCase(method)) {
                handleCreate(exchange);
            } else {
                HttpUtils.sendError(exchange, 405, "Method not allowed");
            }
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        CustomerRequest request = HttpUtils.readJsonBody(exchange, CustomerRequest.class);
        if (request == null || request.name == null || request.phone == null) {
            HttpUtils.sendError(exchange, 400, "name and phone are required");
            return;
        }
        Customer customer = customerService.loginOrRegister(request.name, request.phone);
        HttpUtils.sendJson(exchange, 201, customer);
    }
}