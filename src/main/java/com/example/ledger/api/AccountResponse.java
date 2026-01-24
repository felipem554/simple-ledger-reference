package com.example.ledger.api;

public class AccountResponse {
    private String id;
    private String name;
    private String direction;
    private long balance;

    public AccountResponse(String id, String name, String direction, long balance) {
        this.id = id;
        this.name = name;
        this.direction = direction;
        this.balance = balance;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDirection() {
        return direction;
    }

    public long getBalance() {
        return balance;
    }
}
