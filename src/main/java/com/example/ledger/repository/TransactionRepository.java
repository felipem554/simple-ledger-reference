package com.example.ledger.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TransactionRepository {
    private final JdbcTemplate jdbcTemplate;

    public TransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<TransactionRow> MAPPER = (rs, rowNum) -> new TransactionRow(
        rs.getString("id"),
        rs.getString("name")
    );

    public void insert(TransactionRow transaction) {
        jdbcTemplate.update(
            "INSERT INTO transactions (id, name) VALUES (?, ?)",
            transaction.id(),
            transaction.name()
        );
    }

    public Optional<TransactionRow> findById(String id) {
        List<TransactionRow> rows = jdbcTemplate.query(
            "SELECT id, name FROM transactions WHERE id = ?",
            MAPPER,
            id
        );
        return rows.stream().findFirst();
    }

    public record TransactionRow(String id, String name) {
    }
}
