package com.example.ledger.domain;

public final class Money {

    private final long amount;

    public Money(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }
        this.amount = amount;
    }

    public long amount() {
        return amount;
    }

    public Money plus(Money other) {
        return new Money(Math.addExact(this.amount, other.amount));
    }

    public Money minus(Money other) {
        long result = Math.subtractExact(this.amount, other.amount);
        if (result < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }
        return new Money(result);
    }
}
