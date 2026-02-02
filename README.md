# Simple Ledger (Spring Boot + SQLite)

This service implements a double-entry ledger with balanced transactions, immutable records, and idempotent transaction creation.

## Requirements
- Java 17+
- Gradle 8+

## Running
```bash
./gradlew bootRun
```
The service listens on `http://localhost:5000`.

## Tests
```bash
./gradlew test
```

## Database migrations (Liquibase)

This project uses **Liquibase** to manage schema creation and evolution (tables, constraints, and SQLite triggers).

- Master changelog: `src/main/resources/db/changelog/db.changelog-master.yaml`
- Initial schema: `src/main/resources/db/changelog/sql/001_init.sql`
- Liquibase tables created in SQLite: `DATABASECHANGELOG`, `DATABASECHANGELOGLOCK`

### Reset the local database
If you want a clean DB (useful during development):

```bash
rm -f ledger.db
./gradlew bootRun
```

Liquibase will re-apply migrations automatically on startup.

> Note: Spring SQL init (`schema.sql` / `data.sql`) is disabled (`spring.sql.init.mode=never`) so schema is not created by Spring’s script runner.

## API

### POST /accounts
Creates a new account.

```bash
curl --request POST   --url http://localhost:5000/accounts   --header 'Content-Type: application/json'   --data '{"name":"cash","direction":"debit"}'
```

### GET /accounts/{id}
Fetches an account by id.

### POST /transactions
Creates a balanced transaction. Optionally include `Idempotency-Key` to guard against duplicate requests.

```bash
curl --request POST   --url http://localhost:5000/transactions   --header 'Content-Type: application/json'   --header 'Idempotency-Key: example-key'   --data '{"name":"transfer","entries":[{"direction":"debit","account_id":"...","amount":100},{"direction":"credit","account_id":"...","amount":100}]}'
```

## Notes
- SQLite runs in WAL mode with foreign keys enabled (see `src/main/java/com/example/ledger/config/DatabaseInitializer.java`).
- Immutability is enforced via **SQLite triggers** managed by **Liquibase** (`src/main/resources/db/changelog/sql/001_init.sql`).
- For parallel request handling, run multiple JVM instances behind a load balancer; WAL mode enables concurrent reads/writes.

## Load testing plan

This app is write-heavy (transactions) and correctness-sensitive (idempotency + double-entry balance). A good load test should validate **both** performance and invariants.

### What to measure
- Latency: p50 / p95 / p99 per endpoint
- Throughput (RPS) and error rate
- DB behavior under contention (SQLite write locks)
- App resource usage: CPU, heap, GC pauses, thread pool saturation
- Ledger correctness:
    - transaction is balanced (sum debit == sum credit)
    - idempotency key returns the same transaction and does not duplicate entries
    - no deletes (triggers) and no partial writes

### Recommended approach (quick + practical): k6
k6 is lightweight, easy to run from CLI/CI, and great for HTTP APIs.

#### Scenario design
1) **Warm-up**
- a few minutes at low RPS to warm JVM, classloading, JIT

2) **Mixed steady-state**
- 10–30% reads (`GET /accounts/{id}`)
- 70–90% writes (`POST /transactions`)
- A small percentage of requests re-use the same idempotency key to validate behavior under retries

3) **Ramp test**
- gradually increase VUs/RPS until you see:
    - latency cliff, timeouts, 5xx, or DB lock contention

4) **Soak test**
- hold a moderate load (e.g., 30–60 minutes) to catch resource leaks

#### Example k6 script (baseline)
Create `loadtest/k6-ledger.js`:

```javascript
import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  stages: [
    { duration: "30s", target: 5 },
    { duration: "2m", target: 20 },
    { duration: "2m", target: 50 },
    { duration: "30s", target: 0 },
  ],
  thresholds: {
    http_req_failed: ["rate<0.01"],          // < 1% errors
    http_req_duration: ["p(95)<500"],        // p95 < 500ms (tune for your machine)
  },
};

const BASE = __ENV.BASE_URL || "http://localhost:5000";

const DEBIT_ACCOUNT = __ENV.DEBIT_ACCOUNT_ID;
const CREDIT_ACCOUNT = __ENV.CREDIT_ACCOUNT_ID;

export default function () {
  // 80% transactions, 20% reads
  if (Math.random() < 0.8) {
    const key = Math.random() < 0.05 ? "retry-key-1" : `k-${__VU}-${__ITER}`;

    const payload = JSON.stringify({
      name: "transfer",
      entries: [
        { direction: "debit",  account_id: DEBIT_ACCOUNT,  amount: 100 },
        { direction: "credit", account_id: CREDIT_ACCOUNT, amount: 100 },
      ],
    });

    const res = http.post(`${BASE}/transactions`, payload, {
      headers: {
        "Content-Type": "application/json",
        "Idempotency-Key": key,
      },
    });

    check(res, {
      "tx status is 200/201": (r) => r.status === 200 || r.status === 201,
    });
  } else {
    const res = http.get(`${BASE}/accounts/${DEBIT_ACCOUNT}`);
    check(res, {
      "account status is 200": (r) => r.status === 200,
    });
  }

  sleep(0.1);
}
```

Run it:

```bash
# in one terminal
./gradlew bootRun

# in another terminal:
k6 run   -e BASE_URL=http://localhost:5000   -e DEBIT_ACCOUNT_ID=<id>   -e CREDIT_ACCOUNT_ID=<id>   loadtest/k6-ledger.js
```

### Deeper Java-focused option: Gatling
If you want more detailed JVM-friendly reporting and Java-centric workflows, Gatling is a great follow-up:
- Use feeders (CSV) for account IDs
- Model retries/idempotency explicitly
- Generate HTML reports and trend them in CI

### Interpreting results for SQLite
SQLite has a single-writer constraint; under high write concurrency you may see rising p95/p99 latency and occasional lock-related errors (depending on timeouts). If your target is heavy concurrent writes, consider migrating to Postgres for a more scalable write path.
