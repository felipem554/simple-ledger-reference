package com.example.ledger.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class IdempotencyRepository {
    private final JdbcTemplate jdbcTemplate;

    public IdempotencyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<IdempotencyRow> MAPPER = (rs, rowNum) -> new IdempotencyRow(
        rs.getString("key"),
        rs.getString("request_hash"),
        rs.getString("transaction_id")
    );

    public void insert(IdempotencyRow row) {
        jdbcTemplate.update(
            "INSERT INTO idempotency_keys (key, request_hash, transaction_id) VALUES (?, ?, ?)",
            row.key(),
            row.requestHash(),
            row.transactionId()
        );
    }

    public Optional<IdempotencyRow> findByKey(String key) {
        List<IdempotencyRow> rows = jdbcTemplate.query(
            "SELECT key, request_hash, transaction_id FROM idempotency_keys WHERE key = ?",
            MAPPER,
            key
        );
        return rows.stream().findFirst();
    }

    public record IdempotencyRow(String key, String requestHash, String transactionId) {
    }
}
