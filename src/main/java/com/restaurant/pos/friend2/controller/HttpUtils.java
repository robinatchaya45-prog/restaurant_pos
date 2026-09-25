package com.restaurant.pos.friend2.controller;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Small shared helper for reading/writing JSON over com.sun.net.httpserver.
 * Not a framework - just avoids duplicating the same 10 lines in every
 * HttpHandler. Uses Gson only (no Spring MVC / Jackson-Spring integration).
 */
final class HttpUtils {

    static final Gson GSON = new Gson();

    private HttpUtils() {
    }

    static <T> T readJsonBody(HttpExchange exchange, Class<T> type) throws IOException {
        try (InputStreamReader reader =
                     new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, type);
        }
    }

    static void sendJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        String json = GSON.toJson(body);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    static void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, String> body = new HashMap<>();
        body.put("error", message);
        sendJson(exchange, statusCode, body);
    }

    /** Extracts the numeric path segment after the given prefix, e.g. "/api/orders/42" -> 42. */
    static Long extractIdSegment(String path, String prefix) {
        String remainder = path.substring(prefix.length());
        int slashIndex = remainder.indexOf('/');
        String idPart = slashIndex >= 0 ? remainder.substring(0, slashIndex) : remainder;
        try {
            return Long.parseLong(idPart);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}