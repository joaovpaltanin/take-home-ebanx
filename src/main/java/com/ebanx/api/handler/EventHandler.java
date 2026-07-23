package com.ebanx.api.handler;

import com.ebanx.api.service.AccountService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class EventHandler extends BaseHandler {

    private final AccountService service;

    private final Map<String, EventAction> actions = Map.of(
            "deposit", this::handleDeposit,
            "withdraw", this::handleWithdraw,
            "transfer", this::handleTransfer
    );

    public EventHandler(AccountService service) {
        this.service = service;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

        String type = extractString(body, "type");
        String origin = extractString(body, "origin");
        String destination = extractString(body, "destination");
        Integer amount = extractInt(body, "amount");

        EventAction action = actions.get(type);

        if (action == null) {
            sendResponse(exchange, 400, "0");
            return;
        }

        action.execute(exchange, origin, destination, amount);
    }

    private void handleDeposit(HttpExchange exchange, String origin, String destination, Integer amount) throws IOException {
        int balance = service.deposit(destination, amount);
        String json = String.format(
                "{\"destination\":{\"id\":\"%s\",\"balance\":%d}}",
                destination, balance
        );
        sendResponse(exchange, 201, json);
    }

    private void handleWithdraw(HttpExchange exchange, String origin, String destination, Integer amount) throws IOException {
        if (!service.exists(origin)) {
            sendResponse(exchange, 404, "0");
            return;
        }
        int balance = service.withdraw(origin, amount);
        String json = String.format(
                "{\"origin\":{\"id\":\"%s\",\"balance\":%d}}",
                origin, balance
        );
        sendResponse(exchange, 201, json);
    }

    private void handleTransfer(HttpExchange exchange, String origin, String destination, Integer amount) throws IOException {
        if (!service.exists(origin)) {
            sendResponse(exchange, 404, "0");
            return;
        }
        int originBalance = service.withdraw(origin, amount);
        int destinationBalance = service.deposit(destination, amount);
        String json = String.format(
                "{\"origin\":{\"id\":\"%s\",\"balance\":%d},\"destination\":{\"id\":\"%s\",\"balance\":%d}}",
                origin, originBalance, destination, destinationBalance
        );
        sendResponse(exchange, 201, json);
    }

    @FunctionalInterface
    private interface EventAction {
        void execute(HttpExchange exchange, String origin, String destination, Integer amount) throws IOException;
    }
}