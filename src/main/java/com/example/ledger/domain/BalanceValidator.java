package com.example.ledger.domain;

import java.util.List;

public final class BalanceValidator {
    private BalanceValidator() {
    }

    public static void validateBalanced(List<BalanceEntry> entries) {
        long debitTotal = 0L;
        long creditTotal = 0L;

        for (BalanceEntry entry : entries) {
            if (entry.amount() <= 0) {
                throw new IllegalArgumentException("amount must be positive");
            }
            if (entry.direction() == Direction.DEBIT) {
                debitTotal = Math.addExact(debitTotal, entry.amount());
            } else {
                creditTotal = Math.addExact(creditTotal, entry.amount());
            }
        }

        if (debitTotal != creditTotal) {
            throw new UnbalancedTransactionException("transaction entries must balance");
        }
    }
}
