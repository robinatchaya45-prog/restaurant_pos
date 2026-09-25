package com.restaurant.pos.friend2.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.restaurant.pos.friend2.exception.DatabaseException;
import com.restaurant.pos.friend2.exception.InsufficientStockException;
import com.restaurant.pos.friend2.exception.InvalidQuantityException;
import com.restaurant.pos.friend2.exception.InvalidTableNumberException;
import com.restaurant.pos.friend2.exception.OrderAlreadyCancelledException;
import com.restaurant.pos.friend2.exception.OrderNotEditableException;
import com.restaurant.pos.friend2.model.Order;
import com.restaurant.pos.friend2.model.OrderItem;
import com.restaurant.pos.friend2.service.OrderService;
import com.restaurant.pos.friend2.service.OrderService.CartLine;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Routes HTTP requests under /api/orders to the OrderService.
 * Contains NO business logic and NO SQL - both live in lower layers.
 *
 * Handled routes:
 *   POST   /api/orders                -> place a new order
 *   GET    /api/orders/{id}           -> fetch order + items
 *   PUT    /api/orders/{id}           -> increase/decrease an item's quantity
 *   DELETE /api/orders/{id}           -> cancel the order
 *   POST   /api/orders/{id}/cancel    -> cancel the order
 *   POST   /api/orders/{id}/lock      -> force-check/lock (normally automatic)
 *   GET    /api/orders/{id}/status    -> status + remaining edit time
 */
public class OrderController implements HttpHandler {

    private static final String BASE_PATH = "/api/orders";

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    private static class OrderItemRequest {
        Long itemId;
        Integer quantity;
        BigDecimal price;
    }

    private static class PlaceOrderRequest {
        Long customerId;
        Integer tableNo;
        List<OrderItemRequest> items;
    }

    private static class EditItemRequest {
        Long itemId;
        Integer quantityChange; // positive = increase, negative = decrease
    }

    private static class OrderResponse {
        Order order;
        List<OrderItem> items;

        OrderResponse(Order order, List<OrderItem> items) {
            this.order = order;
            this.items = items;
        }
    }

    private static class StatusResponse {
        Long orderId;
        Integer tableNo;
        String status;
        long remainingEditSeconds;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if (path.equals(BASE_PATH) && "POST".equalsIgnoreCase(method)) {
                handlePlaceOrder(exchange);
            } else if (path.endsWith("/cancel") && "POST".equalsIgnoreCase(method)) {
                handleCancel(exchange, path);
            } else if (path.endsWith("/lock") && "POST".equalsIgnoreCase(method)) {
                handleLock(exchange, path);
            } else if (path.endsWith("/status") && "GET".equalsIgnoreCase(method)) {
                handleStatus(exchange, path);
            } else if ("GET".equalsIgnoreCase(method)) {
                handleGet(exchange, path);
            } else if ("PUT".equalsIgnoreCase(method)) {
                handleEdit(exchange, path);
            } else if ("DELETE".equalsIgnoreCase(method)) {
                handleCancel(exchange, path);
            } else {
                HttpUtils.sendError(exchange, 405, "Method not allowed");
            }
        } catch (InvalidTableNumberException | InvalidQuantityException e) {
            HttpUtils.sendError(exchange, 400, e.getMessage());
        } catch (InsufficientStockException e) {
            HttpUtils.sendError(exchange, 409, e.getMessage());
        } catch (OrderNotEditableException | OrderAlreadyCancelledException e) {
            HttpUtils.sendError(exchange, 409, e.getMessage());
        } catch (DatabaseException e) {
            HttpUtils.sendError(exchange, 500, e.getMessage());
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    private void handlePlaceOrder(HttpExchange exchange) throws IOException {
        PlaceOrderRequest request = HttpUtils.readJsonBody(exchange, PlaceOrderRequest.class);
        if (request == null || request.items == null || request.items.isEmpty()) {
            HttpUtils.sendError(exchange, 400, "customerId, tableNo and a non-empty items list are required");
            return;
        }

        List<CartLine> cartLines = new ArrayList<>();
        for (OrderItemRequest item : request.items) {
            cartLines.add(new CartLine(item.itemId, item.quantity, item.price));
        }

        Order order = orderService.placeOrder(request.customerId, request.tableNo, cartLines);
        List<OrderItem> items = orderService.findItems(order.getOrderId());
        HttpUtils.sendJson(exchange, 201, new OrderResponse(order, items));
    }

    private void handleGet(HttpExchange exchange, String path) throws IOException {
        Long orderId = HttpUtils.extractIdSegment(path, BASE_PATH + "/");
        if (orderId == null) {
            HttpUtils.sendError(exchange, 400, "Invalid order id");
            return;
        }
        Optional<Order> maybeOrder = orderService.findById(orderId);
        if (maybeOrder.isEmpty()) {
            HttpUtils.sendError(exchange, 404, "Order not found: " + orderId);
            return;
        }
        List<OrderItem> items = orderService.findItems(orderId);
        HttpUtils.sendJson(exchange, 200, new OrderResponse(maybeOrder.get(), items));
    }

    private void handleEdit(HttpExchange exchange, String path) throws IOException {
        Long orderId = HttpUtils.extractIdSegment(path, BASE_PATH + "/");
        if (orderId == null) {
            HttpUtils.sendError(exchange, 400, "Invalid order id");
            return;
        }
        EditItemRequest request = HttpUtils.readJsonBody(exchange, EditItemRequest.class);
        if (request == null || request.itemId == null || request.quantityChange == null
                || request.quantityChange == 0) {
            HttpUtils.sendError(exchange, 400, "itemId and a non-zero quantityChange are required");
            return;
        }

        if (request.quantityChange > 0) {
            orderService.increaseItemQuantity(orderId, request.itemId, request.quantityChange);
        } else {
            orderService.decreaseItemQuantity(orderId, request.itemId, -request.quantityChange);
        }

        Order order = orderService.findById(orderId).orElse(null);
        List<OrderItem> items = orderService.findItems(orderId);
        HttpUtils.sendJson(exchange, 200, new OrderResponse(order, items));
    }

    private void handleCancel(HttpExchange exchange, String path) throws IOException {
        Long orderId = extractOrderIdBeforeSuffix(path);
        if (orderId == null) {
            HttpUtils.sendError(exchange, 400, "Invalid order id");
            return;
        }
        orderService.cancelOrder(orderId);
        Map<String, Object> body = new HashMap<>();
        body.put("orderId", orderId);
        body.put("status", "CANCELLED");
        HttpUtils.sendJson(exchange, 200, body);
    }

    private void handleLock(HttpExchange exchange, String path) throws IOException {
        Long orderId = extractOrderIdBeforeSuffix(path);
        if (orderId == null) {
            HttpUtils.sendError(exchange, 400, "Invalid order id");
            return;
        }
        orderService.lockOrder(orderId);
        Order order = orderService.findById(orderId).orElse(null);
        HttpUtils.sendJson(exchange, 200, order);
    }

    private void handleStatus(HttpExchange exchange, String path) throws IOException {
        Long orderId = extractOrderIdBeforeSuffix(path);
        if (orderId == null) {
            HttpUtils.sendError(exchange, 400, "Invalid order id");
            return;
        }
        Order order = orderService.findById(orderId).orElse(null);
        if (order == null) {
            HttpUtils.sendError(exchange, 404, "Order not found: " + orderId);
            return;
        }
        StatusResponse response = new StatusResponse();
        response.orderId = order.getOrderId();
        response.tableNo = order.getTableNo();
        response.status = order.getStatus().name();
        response.remainingEditSeconds = orderService.remainingEditSeconds(order);
        HttpUtils.sendJson(exchange, 200, response);
    }

    /** Extracts the id from paths like "/api/orders/42/cancel" -> 42. */
    private Long extractOrderIdBeforeSuffix(String path) {
        String withoutBase = path.substring(BASE_PATH.length() + 1);
        int slashIndex = withoutBase.indexOf('/');
        String idPart = slashIndex >= 0 ? withoutBase.substring(0, slashIndex) : withoutBase;
        try {
            return Long.parseLong(idPart);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}