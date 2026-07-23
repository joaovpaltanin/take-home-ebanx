package com.ebanx.api.handler;

import com.ebanx.api.exception.BadRequestException;
import com.ebanx.api.exception.NotFoundException;
import com.ebanx.api.service.AccountService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class BalanceHandler extends BaseHandler {

    private final AccountService service;

    public BalanceHandler(AccountService service) {
        this.service = service;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getRawQuery();
        String rawAccountId = extractQueryParam(query, "account_id");

        try {
            if (!isPositiveInteger(rawAccountId)) {
                throw new BadRequestException();
            }

            Integer balance = service.getBalance(rawAccountId);
            sendResponse(exchange, 200, balance.toString());
        } catch (BadRequestException e) {
            sendResponse(exchange, 400, "0");
        } catch (NotFoundException e) {
            sendResponse(exchange, 404, "0");
        }
    }
}