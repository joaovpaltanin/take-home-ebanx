package com.ebanx.api;

import com.ebanx.api.handler.BalanceHandler;
import com.ebanx.api.service.AccountService;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) throws IOException {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        AccountService service = new AccountService();
        server.createContext("/balance", new BalanceHandler(service));

        server.setExecutor(null);

        server.start();
        System.out.println("Servidor rodando em http://localhost:" + port);
    }
}