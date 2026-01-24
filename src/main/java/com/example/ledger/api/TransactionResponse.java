package com.example.ledger.api;

import java.util.List;

public class TransactionResponse {
    private final String id;
    private final String name;
    private final List<EntryResponse> entries;

    public TransactionResponse(String id, String name, List<EntryResponse> entries) {
        this.id = id;
        this.name = name;
        this.entries = entries;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<EntryResponse> getEntries() {
        return entries;
    }
}
