import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  stages: [
    { duration: "30s", target: 5 },   // warmup
    { duration: "2m", target: 20 },   // steady
    { duration: "2m", target: 50 },   // ramp
    { duration: "30s", target: 0 },   // cool down
  ],
  thresholds: {
    http_req_failed: ["rate<0.01"],       // <1% errors
    http_req_duration: ["p(95)<800"],     // tune for your machine + SQLite
  },
};

const BASE = __ENV.BASE_URL || "http://localhost:5000";
const DEBIT_ACCOUNT = __ENV.DEBIT_ACCOUNT_ID;
const CREDIT_ACCOUNT = __ENV.CREDIT_ACCOUNT_ID;

function postTransaction(idempotencyKey) {
  const payload = JSON.stringify({
    name: "transfer",
    entries: [
      { direction: "debit", account_id: DEBIT_ACCOUNT, amount: 100 },
      { direction: "credit", account_id: CREDIT_ACCOUNT, amount: 100 },
    ],
  });

  return http.post(`${BASE}/transactions`, payload, {
    headers: {
      "Content-Type": "application/json",
      "Idempotency-Key": idempotencyKey,
    },
  });
}

export default function () {
  // 80% writes, 20% reads
  if (Math.random() < 0.8) {
    // 5% retry traffic uses same key (tests idempotency)
    const key =
      Math.random() < 0.05 ? "retry-key-1" : `k-${__VU}-${__ITER}-${Date.now()}`;

    const res = postTransaction(key);

    check(res, {
      "tx status is 200/201": (r) => r.status === 200 || r.status === 201,
    });
  } else {
    const res = http.get(`${BASE}/accounts/${DEBIT_ACCOUNT}`);
    check(res, {
      "account status is 200": (r) => r.status === 200,
    });
  }

  sleep(0.05);
}
