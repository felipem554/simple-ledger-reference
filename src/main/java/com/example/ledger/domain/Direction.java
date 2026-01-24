package com.example.ledger.domain;

public enum Direction {
    DEBIT("debit"),
    CREDIT("credit");

    private final String value;

    Direction(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static Direction from(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("direction is required");
        }
        return switch (raw.toLowerCase()) {
            case "debit" -> DEBIT;
            case "credit" -> CREDIT;
            default -> throw new IllegalArgumentException("invalid direction: " + raw);
        };
    }
}
