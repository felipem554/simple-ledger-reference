package com.example.ledger.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class TransactionRequest {
    private String id;
    private String name;

    @NotNull
    @Size(min = 1)
    private List<@Valid EntryRequest> entries;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<EntryRequest> getEntries() {
        return entries;
    }

    public void setEntries(List<EntryRequest> entries) {
        this.entries = entries;
    }
}
