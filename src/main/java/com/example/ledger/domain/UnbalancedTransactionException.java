package com.example.ledger.domain;

public class UnbalancedTransactionException extends IllegalArgumentException {
    public UnbalancedTransactionException(String message) {
        super(message);
    }
}
