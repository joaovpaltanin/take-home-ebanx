package com.ebanx.api;

import com.ebanx.api.handler.BalanceHandler;
import com.ebanx.api.handler.EventHandler;
import com.ebanx.api.handler.ResetHandler;
import com.ebanx.api.service.AccountService;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) throws IOException {
        AccountService service = new AccountService();

        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/balance", new BalanceHandler(service));
        server.createContext("/event", new EventHandler(service));
        server.createContext("/reset", new ResetHandler(service));
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();

        System.out.println("Server running on port " + port);
    }
}