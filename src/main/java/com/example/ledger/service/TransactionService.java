package com.example.ledger.service;

import com.example.ledger.api.EntryRequest;
import com.example.ledger.api.EntryResponse;
import com.example.ledger.api.TransactionRequest;
import com.example.ledger.api.TransactionResponse;
import com.example.ledger.domain.BalanceEntry;
import com.example.ledger.domain.BalanceValidator;
import com.example.ledger.domain.Direction;
import com.example.ledger.domain.UnbalancedTransactionException;
import com.example.ledger.exception.ApiException;
import com.example.ledger.repository.AccountRepository;
import com.example.ledger.repository.EntryRepository;
import com.example.ledger.repository.IdempotencyRepository;
import com.example.ledger.repository.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final EntryRepository entryRepository;
    private final AccountRepository accountRepository;
    private final IdempotencyRepository idempotencyRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              EntryRepository entryRepository,
                              AccountRepository accountRepository,
                              IdempotencyRepository idempotencyRepository) {
        this.transactionRepository = transactionRepository;
        this.entryRepository = entryRepository;
        this.accountRepository = accountRepository;
        this.idempotencyRepository = idempotencyRepository;
    }

    @Transactional
    public TransactionResponse create(TransactionRequest request, String idempotencyKey) {
        List<EntryRequest> entries = request.getEntries();

        validateBalanced(entries);

        String requestId = request.getId();
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String hash = computeRequestHash(requestId, request.getName(), entries);
            Optional<IdempotencyRepository.IdempotencyRow> existing = idempotencyRepository.findByKey(idempotencyKey);
            if (existing.isPresent()) {
                IdempotencyRepository.IdempotencyRow row = existing.get();
                if (!row.requestHash().equals(hash)) {
                    throw new ApiException(HttpStatus.CONFLICT, "idempotency_conflict", "idempotency key reused with different payload");
                }
                return fetchResponse(row.transactionId());
            }
            String transactionId = requestId == null ? UUID.randomUUID().toString() : requestId;
            return createTransactionWithIdempotency(transactionId, request, hash, idempotencyKey);
        }

        String transactionId = requestId == null ? UUID.randomUUID().toString() : requestId;
        return createTransaction(transactionId, request);
    }

    private TransactionResponse createTransactionWithIdempotency(String transactionId, TransactionRequest request, String hash, String key) {
        TransactionResponse response = createTransaction(transactionId, request);
        idempotencyRepository.insert(new IdempotencyRepository.IdempotencyRow(key, hash, transactionId));
        return response;
    }

    private TransactionResponse createTransaction(String transactionId, TransactionRequest request) {
        Optional<TransactionRepository.TransactionRow> existing = transactionRepository.findById(transactionId);
        if (existing.isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "transaction_exists", "transaction already exists");
        }

        Map<String, AccountRepository.AccountRow> accounts = loadAccounts(request.getEntries());
        Map<String, Long> newBalances = new HashMap<>();

        List<EntryResponse> entryResponses = new ArrayList<>();
        for (EntryRequest entry : request.getEntries()) {
            String entryId = entry.getId() == null ? UUID.randomUUID().toString() : entry.getId();
            Direction entryDirection = Direction.from(entry.getDirection());
            long amount = entry.getAmount();
            if (amount <= 0) {
                throw new IllegalArgumentException("amount must be positive");
            }

            AccountRepository.AccountRow account = accounts.get(entry.getAccountId());
            long currentBalance = newBalances.getOrDefault(account.id(), account.balance());
            long delta = entryDirection == account.direction() ? amount : -amount;
            long updatedBalance = Math.addExact(currentBalance, delta);
            newBalances.put(account.id(), updatedBalance);

            entryResponses.add(new EntryResponse(entryId, entry.getAccountId(), entryDirection.value(), amount));
        }

        transactionRepository.insert(new TransactionRepository.TransactionRow(transactionId, request.getName()));

        for (EntryResponse entry : entryResponses) {
            entryRepository.insert(new EntryRepository.EntryRow(
                entry.getId(),
                transactionId,
                entry.getAccountId(),
                Direction.from(entry.getDirection()),
                entry.getAmount()
            ));
        }

        for (Map.Entry<String, Long> update : newBalances.entrySet()) {
            accountRepository.updateBalance(update.getKey(), update.getValue());
        }

        return new TransactionResponse(transactionId, request.getName(), entryResponses);
    }

    private void validateBalanced(List<EntryRequest> entries) {
        List<BalanceEntry> balanceEntries = entries.stream()
            .map(entry -> new BalanceEntry(Direction.from(entry.getDirection()), entry.getAmount()))
            .toList();
        try {
            BalanceValidator.validateBalanced(balanceEntries);
        } catch (UnbalancedTransactionException ex) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "unbalanced_transaction", ex.getMessage());
        }
    }

    private Map<String, AccountRepository.AccountRow> loadAccounts(List<EntryRequest> entries) {
        List<String> accountIds = entries.stream().map(EntryRequest::getAccountId).distinct().toList();
        List<AccountRepository.AccountRow> accounts = accountRepository.findByIds(accountIds);
        if (accounts.size() != accountIds.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "account_missing", "one or more accounts do not exist");
        }
        Map<String, AccountRepository.AccountRow> map = new HashMap<>();
        for (AccountRepository.AccountRow account : accounts) {
            map.put(account.id(), account);
        }
        return map;
    }

    private TransactionResponse fetchResponse(String transactionId) {
        TransactionRepository.TransactionRow transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "transaction_not_found", "transaction not found"));
        List<EntryRepository.EntryRow> entries = entryRepository.findByTransactionId(transactionId);
        List<EntryResponse> responses = entries.stream()
            .map(entry -> new EntryResponse(entry.id(), entry.accountId(), entry.direction().value(), entry.amount()))
            .toList();
        return new TransactionResponse(transaction.id(), transaction.name(), responses);
    }

    private String computeRequestHash(String transactionId, String name, List<EntryRequest> entries) {
        StringBuilder builder = new StringBuilder();
        builder.append(transactionId == null ? "" : transactionId).append('|').append(name == null ? "" : name).append('|');
        for (EntryRequest entry : entries) {
            builder.append(entry.getId() == null ? "" : entry.getId())
                .append(',')
                .append(entry.getAccountId())
                .append(',')
                .append(entry.getDirection())
                .append(',')
                .append(entry.getAmount())
                .append(';');
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(builder.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("unable to compute hash", ex);
        }
    }
}
