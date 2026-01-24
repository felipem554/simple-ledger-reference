package com.example.ledger.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EntryResponse {
    private final String id;
    @JsonProperty("account_id")
    private final String accountId;
    private final String direction;
    private final long amount;

    public EntryResponse(String id, String accountId, String direction, long amount) {
        this.id = id;
        this.accountId = accountId;
        this.direction = direction;
        this.amount = amount;
    }

    public String getId() {
        return id;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getDirection() {
        return direction;
    }

    public long getAmount() {
        return amount;
    }
}
