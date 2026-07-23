package com.ebanx.api.handler;

import com.ebanx.api.service.AccountService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class ResetHandler extends BaseHandler {
    private final AccountService service;

    public ResetHandler(AccountService service) {
        this.service = service;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        service.reset();
        sendResponse(exchange, 200, "OK");
    }
}