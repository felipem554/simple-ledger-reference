package com.example.ledger.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyTest {

    @Test
    void rejectsNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> new Money(-1));
    }

    @Test
    void addsAmountsSafely() {
        Money result = new Money(50).plus(new Money(25));
        assertEquals(75, result.amount());
    }
}
