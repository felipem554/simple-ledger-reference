package com.example.ledger.api;

import com.example.ledger.service.AccountService;
import com.example.ledger.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/")
public class LedgerController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    public LedgerController(AccountService accountService, TransactionService transactionService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(@Valid @RequestBody AccountRequest request) {
        return accountService.create(request);
    }

    @GetMapping("/accounts/{id}")
    public AccountResponse getAccount(@PathVariable String id) {
        return accountService.getById(id);
    }

    @GetMapping("/accounts")
    public List<AccountResponse> getAllAccounts() {
        return accountService.getAll();
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createTransaction(
        @Valid @RequestBody TransactionRequest request,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return transactionService.create(request, idempotencyKey);
    }
}
