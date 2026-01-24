package com.example.ledger.service;

import com.example.ledger.api.AccountRequest;
import com.example.ledger.api.AccountResponse;
import com.example.ledger.domain.Direction;
import com.example.ledger.exception.ApiException;
import com.example.ledger.repository.AccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class AccountService {
    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountResponse create(AccountRequest request) {
        String id = request.getId() == null ? UUID.randomUUID().toString() : request.getId();
        Direction direction = Direction.from(request.getDirection());
        long balance = request.getBalance() == null ? 0L : request.getBalance();
        if (balance < 0) {
            throw new IllegalArgumentException("balance must be non-negative");
        }

        Optional<AccountRepository.AccountRow> existing = accountRepository.findById(id);
        if (existing.isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "account_exists", "account already exists");
        }

        AccountRepository.AccountRow row = new AccountRepository.AccountRow(
            id,
            request.getName(),
            direction,
            balance
        );
        accountRepository.insert(row);

        return new AccountResponse(id, request.getName(), direction.value(), balance);
    }

    public AccountResponse getById(String id) {
        AccountRepository.AccountRow row = accountRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "account_not_found", "account not found"));

        return new AccountResponse(row.id(), row.name(), row.direction().value(), row.balance());
    }
}
