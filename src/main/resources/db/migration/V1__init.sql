-- ---------------------------------------------------------------------------
-- wallets: 소유자(owner_user_id) 포함, 잔액 비음수 제약
-- ---------------------------------------------------------------------------
CREATE TABLE wallets (
    id              BIGSERIAL   PRIMARY KEY,
    owner_user_id   BIGINT      NOT NULL,
    balance         BIGINT      NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT chk_wallets_balance_non_negative CHECK (balance >= 0)
);

CREATE INDEX idx_wallets_owner_user_id
    ON wallets (owner_user_id);

COMMENT ON TABLE wallets IS 'Wallet';
COMMENT ON COLUMN wallets.id IS 'Wallet 고유 식별자';
COMMENT ON COLUMN wallets.owner_user_id IS 'Wallet 소유자 사용자 ID';
COMMENT ON COLUMN wallets.balance IS '현재 잔액';
COMMENT ON COLUMN wallets.created_at IS '생성 시각';
COMMENT ON COLUMN wallets.updated_at IS '수정 시각';

-- ---------------------------------------------------------------------------
-- transactions: 결과 스냅샷 저장, 멱등성(transaction_id UNIQUE)
-- ---------------------------------------------------------------------------
CREATE TABLE transactions (
    id              BIGSERIAL   PRIMARY KEY,
    transaction_id  VARCHAR(64) NOT NULL,
    wallet_id       BIGINT      NOT NULL,
    amount          BIGINT      NOT NULL,
    balance_after   BIGINT,
    status          VARCHAR(16) NOT NULL,
    failure_reason  VARCHAR(64),
    created_at      TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT ux_transactions_transaction_id UNIQUE (transaction_id),
    CONSTRAINT chk_transactions_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_transactions_status_valid CHECK (status IN ('SUCCESS', 'FAILED'))
);

CREATE INDEX idx_transactions_wallet_id
    ON transactions (wallet_id);

CREATE INDEX idx_transactions_wallet_created_at
    ON transactions (wallet_id, created_at);

COMMENT ON TABLE transactions IS 'Wallet 출금 트랜잭션 기록';
COMMENT ON COLUMN transactions.id IS '트랜잭션 PK';
COMMENT ON COLUMN transactions.transaction_id IS '멱등성 보장을 위한 클라이언트 요청 식별자';
COMMENT ON COLUMN transactions.wallet_id IS '출금 대상 Wallet ID';
COMMENT ON COLUMN transactions.amount IS '출금 금액';
COMMENT ON COLUMN transactions.balance_after IS '처리 후 잔액 (실패 시 NULL 가능)';
COMMENT ON COLUMN transactions.status IS '처리 결과 (SUCCESS / FAILED)';
COMMENT ON COLUMN transactions.failure_reason IS '실패 사유 코드';
COMMENT ON COLUMN transactions.created_at IS '생성 시각';
COMMENT ON COLUMN transactions.updated_at IS '수정 시각';
