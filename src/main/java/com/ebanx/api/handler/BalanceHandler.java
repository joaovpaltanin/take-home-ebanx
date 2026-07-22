package com.ebanx.api.handler;

import com.ebanx.api.exception.NotFoundException;
import com.ebanx.api.service.AccountService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class BalanceHandler implements HttpHandler {

    private final AccountService service;

    public BalanceHandler(AccountService service) {
        this.service = service;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "0");
            return;
        }

        String query = exchange.getRequestURI().getRawQuery();
        String rawAccountId = extractParam(query, "account_id");

        if (!isPositiveInteger(rawAccountId)) {
            sendResponse(exchange, 400, "0");
            return;
        }

        try {
            Integer balance = service.getBalance(rawAccountId);
            String response = String.format("{\"account_id\": %s, \"balance\": %.2f}", rawAccountId, balance);
            sendResponse(exchange, 200, response);
        } catch (NotFoundException e) {
            sendResponse(exchange, 404, "0");
        }
    }

    private String extractParam(String query, String paramName) {
        if (query == null) return null;

        String prefix = paramName + "=";
        if (!query.startsWith(prefix)) return null;

        return query.substring(prefix.length());
    }

    private boolean isPositiveInteger(String value) {
        return value != null && value.matches("^[1-9]\\d*$");
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}