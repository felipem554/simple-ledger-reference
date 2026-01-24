package com.example.ledger.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BalanceValidatorTest {

    @Test
    void acceptsBalancedEntries() {
        List<BalanceEntry> entries = List.of(
            new BalanceEntry(Direction.DEBIT, 100),
            new BalanceEntry(Direction.CREDIT, 100)
        );

        assertDoesNotThrow(() -> BalanceValidator.validateBalanced(entries));
    }

    @Test
    void rejectsUnbalancedEntries() {
        List<BalanceEntry> entries = List.of(
            new BalanceEntry(Direction.DEBIT, 100),
            new BalanceEntry(Direction.CREDIT, 90)
        );

        assertThrows(IllegalArgumentException.class, () -> BalanceValidator.validateBalanced(entries));
    }
}
