package com.example.ledger.repository;

import com.example.ledger.domain.Direction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EntryRepository {
    private final JdbcTemplate jdbcTemplate;

    public EntryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<EntryRow> MAPPER = (rs, rowNum) -> new EntryRow(
        rs.getString("id"),
        rs.getString("transaction_id"),
        rs.getString("account_id"),
        Direction.from(rs.getString("direction")),
        rs.getLong("amount")
    );

    public void insert(EntryRow entry) {
        jdbcTemplate.update(
            "INSERT INTO entries (id, transaction_id, account_id, direction, amount) VALUES (?, ?, ?, ?, ?)",
            entry.id(),
            entry.transactionId(),
            entry.accountId(),
            entry.direction().value(),
            entry.amount()
        );
    }

    public List<EntryRow> findByTransactionId(String transactionId) {
        return jdbcTemplate.query(
            "SELECT id, transaction_id, account_id, direction, amount FROM entries WHERE transaction_id = ?",
            MAPPER,
            transactionId
        );
    }

    public record EntryRow(String id, String transactionId, String accountId, Direction direction, long amount) {
    }
}
