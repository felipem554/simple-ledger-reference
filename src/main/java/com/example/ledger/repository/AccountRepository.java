package com.example.ledger.repository;

import com.example.ledger.domain.Direction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AccountRepository {
    private final JdbcTemplate jdbcTemplate;

    public AccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<AccountRow> MAPPER = (rs, rowNum) -> new AccountRow(
        rs.getString("id"),
        rs.getString("name"),
        Direction.from(rs.getString("direction")),
        rs.getLong("balance")
    );

    public void insert(AccountRow account) {
        jdbcTemplate.update(
            "INSERT INTO accounts (id, name, direction, balance) VALUES (?, ?, ?, ?)",
            account.id(),
            account.name(),
            account.direction().value(),
            account.balance()
        );
    }

    public Optional<AccountRow> findById(String id) {
        List<AccountRow> rows = jdbcTemplate.query(
            "SELECT id, name, direction, balance FROM accounts WHERE id = ?",
            MAPPER,
            id
        );
        return rows.stream().findFirst();
    }

    public List<AccountRow> findByIds(List<String> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", ids.stream().map(id -> "?").toList());
        String sql = "SELECT id, name, direction, balance FROM accounts WHERE id IN (" + placeholders + ")";
        return jdbcTemplate.query(sql, MAPPER, ids.toArray());
    }

    public List<AccountRow> findAll() {
        String sql = "SELECT id, name, direction, balance FROM accounts ORDER BY name ASC";
        return jdbcTemplate.query(sql, MAPPER);
    }

    public void updateBalance(String id, long balance) {
        jdbcTemplate.update("UPDATE accounts SET balance = ? WHERE id = ?", balance, id);
    }

    public record AccountRow(String id, String name, Direction direction, long balance) {
    }
}
