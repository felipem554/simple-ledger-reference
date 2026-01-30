CREATE TABLE IF NOT EXISTS accounts (
    id TEXT PRIMARY KEY,
    name TEXT,
    direction TEXT NOT NULL CHECK (direction IN ('debit', 'credit')),
    balance INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS transactions (
    id TEXT PRIMARY KEY,
    name TEXT,
    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);

CREATE TABLE IF NOT EXISTS entries (
    id TEXT PRIMARY KEY,
    transaction_id TEXT NOT NULL,
    account_id TEXT NOT NULL,
    direction TEXT NOT NULL CHECK (direction IN ('debit', 'credit')),
    amount INTEGER NOT NULL CHECK (amount >= 0),
    FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE RESTRICT,
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS idempotency_keys (
    key TEXT PRIMARY KEY,
    request_hash TEXT NOT NULL,
    transaction_id TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE RESTRICT
);

CREATE TRIGGER IF NOT EXISTS prevent_account_delete
 BEFORE DELETE ON accounts
 BEGIN
     SELECT RAISE(FAIL, 'accounts are immutable');
 END;

 CREATE TRIGGER IF NOT EXISTS prevent_transaction_delete
 BEFORE DELETE ON transactions
 BEGIN
     SELECT RAISE(FAIL, 'transactions are immutable');
 END;

 CREATE TRIGGER IF NOT EXISTS prevent_entry_delete
 BEFORE DELETE ON entries
 BEGIN
     SELECT RAISE(FAIL, 'entries are immutable');
 END;
