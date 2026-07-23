package com.ebanx.api.service;

import com.ebanx.api.exception.NotFoundException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AccountService {
    private final Map<String, Integer> accounts = new ConcurrentHashMap<>();

    public int getBalance(String id) {
        Integer balance = accounts.get(id);

        if (balance == null) throw new NotFoundException();

        return balance;
    }
}