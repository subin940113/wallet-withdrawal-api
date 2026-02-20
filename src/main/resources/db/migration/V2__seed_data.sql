-- ---------------------------------------------------------------------------
-- 로컬/개발용 초기 데이터 (테스트 및 HTTP 클라이언트에서 사용)
-- ---------------------------------------------------------------------------

-- 사용자 1: 지갑 1개 (잔액 100,000)
INSERT INTO wallets (owner_user_id, balance, created_at, updated_at)
VALUES (1, 100000, now(), now());

-- 사용자 2: 지갑 1개 (잔액 50,000)
INSERT INTO wallets (owner_user_id, balance, created_at, updated_at)
VALUES (2, 50000, now(), now());

-- 사용자 1: 지갑 1개 추가 (잔액 200,000) - 여러 지갑 보유 시나리오
INSERT INTO wallets (owner_user_id, balance, created_at, updated_at)
VALUES (1, 200000, now(), now());
