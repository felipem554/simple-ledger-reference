# Simple Ledger (Spring Boot + SQLite)

This service implements a double-entry ledger with balanced transactions, immutable records, and idempotent transaction creation.

## Requirements
- Java 17+
- Gradle 8+

## Running
```bash
gradle bootRun
```
The service listens on `http://localhost:5000`.

## Tests
```bash
gradle test
```

## API
### POST /accounts
Creates a new account.

```bash
curl --request POST \
  --url http://localhost:5000/accounts \
  --header 'Content-Type: application/json' \
  --data '{"name":"cash","direction":"debit"}'
```

### GET /accounts/{id}
Fetches an account by id.

### POST /transactions
Creates a balanced transaction. Optionally include `Idempotency-Key` to guard against duplicate requests.

```bash
curl --request POST \
  --url http://localhost:5000/transactions \
  --header 'Content-Type: application/json' \
  --header 'Idempotency-Key: example-key' \
  --data '{"name":"transfer","entries":[{"direction":"debit","account_id":"...","amount":100},{"direction":"credit","account_id":"...","amount":100}]}'
```

## Notes
- SQLite runs in WAL mode with foreign keys enabled (see `src/main/java/com/example/ledger/config/DatabaseInitializer.java`).
- Immutability is enforced via SQLite triggers in `src/main/resources/schema.sql`.
 - For parallel request handling, run multiple JVM instances behind a load balancer; WAL mode enables concurrent reads/writes.
