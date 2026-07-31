package com.ebanx.api.service;

import com.ebanx.api.exception.BadRequestException;
import com.ebanx.api.exception.InsufficientFundsException;
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

    public boolean exists(String id) {
        return accounts.containsKey(id);
    }

    public int deposit(String id, int amount) {
        return accounts.merge(id, amount, Integer::sum);
    }

    public int withdraw(String id, int amount) {
        Integer updatedBalance = accounts.computeIfPresent(id, (accountId, balance) -> {
            if (balance < amount) throw new InsufficientFundsException();
            return balance - amount;
        });

        if (updatedBalance == null) throw new NotFoundException();

        return updatedBalance;
    }

    public synchronized TransferResult transfer(String origin, String destination, int amount) {
        requireAccountId(origin);
        requireAccountId(destination);

        int originBalance = withdraw(origin, amount);
        int destinationBalance = deposit(destination, amount);

        return new TransferResult(originBalance, destinationBalance);
    }

    private static void requireAccountId(String id) {
        if (id == null || id.isBlank()) throw new BadRequestException();
    }

    public void reset() {
        accounts.clear();
    }
}