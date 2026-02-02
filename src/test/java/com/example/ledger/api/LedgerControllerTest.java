package com.example.ledger.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LedgerControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createsAccountAndTransaction() {

        AccountRequest accountA = new AccountRequest();
        accountA.setName("cash");
        accountA.setDirection("debit");
        ResponseEntity<AccountResponse> accountAResponse = restTemplate.postForEntity("/accounts", accountA, AccountResponse.class);
        assertEquals(HttpStatus.CREATED, accountAResponse.getStatusCode());
        assertNotNull(accountAResponse.getBody());

        AccountRequest accountB = new AccountRequest();
        accountB.setName("revenue");
        accountB.setDirection("credit");
        ResponseEntity<AccountResponse> accountBResponse = restTemplate.postForEntity("/accounts", accountB, AccountResponse.class);
        assertEquals(HttpStatus.CREATED, accountBResponse.getStatusCode());
        assertNotNull(accountBResponse.getBody());

        EntryRequest debit = new EntryRequest();
        debit.setAccountId(accountAResponse.getBody().getId());
        debit.setDirection("debit");
        debit.setAmount(100L);

        EntryRequest credit = new EntryRequest();
        credit.setAccountId(accountBResponse.getBody().getId());
        credit.setDirection("credit");
        credit.setAmount(100L);

        TransactionRequest transactionRequest = new TransactionRequest();
        transactionRequest.setName("sale");
        transactionRequest.setEntries(List.of(debit, credit));

        ResponseEntity<TransactionResponse> transactionResponse = restTemplate.postForEntity(
            "/transactions",
            transactionRequest,
            TransactionResponse.class
        );

        assertEquals(HttpStatus.CREATED, transactionResponse.getStatusCode());
        assertNotNull(transactionResponse.getBody());
        assertEquals(2, transactionResponse.getBody().getEntries().size());
    }

    @Test
    void enforcesIdempotencyKey() {

        AccountRequest accountA = new AccountRequest();
        accountA.setName("cash");
        accountA.setDirection("debit");
        AccountResponse accountAResponse = restTemplate.postForEntity("/accounts", accountA, AccountResponse.class).getBody();

        AccountRequest accountB = new AccountRequest();
        accountB.setName("revenue");
        accountB.setDirection("credit");
        AccountResponse accountBResponse = restTemplate.postForEntity("/accounts", accountB, AccountResponse.class).getBody();

        EntryRequest debit = new EntryRequest();
        debit.setAccountId(accountAResponse.getId());
        debit.setDirection("debit");
        debit.setAmount(40L);

        EntryRequest credit = new EntryRequest();
        credit.setAccountId(accountBResponse.getId());
        credit.setDirection("credit");
        credit.setAmount(40L);

        TransactionRequest transactionRequest = new TransactionRequest();
        transactionRequest.setName("repeatable");
        transactionRequest.setEntries(List.of(debit, credit));

        HttpHeaders headers = new HttpHeaders();
        headers.add("Idempotency-Key", "repeatable-key");
        HttpEntity<TransactionRequest> entity = new HttpEntity<>(transactionRequest, headers);

        ResponseEntity<TransactionResponse> first = restTemplate.exchange(
            "/transactions",
            HttpMethod.POST,
            entity,
            TransactionResponse.class
        );
        ResponseEntity<TransactionResponse> second = restTemplate.exchange(
            "/transactions",
            HttpMethod.POST,
            entity,
            TransactionResponse.class
        );

        assertEquals(first.getBody().getId(), second.getBody().getId());
    }
}
