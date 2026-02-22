# 부하테스트 (k6)

출금 API를 대상으로 한 k6 부하테스트입니다.  
- **Scenario A** : 동일 walletId에 서로 다른 transactionId로 요청합니다. (핫키 경합)  
- **Scenario B** : 동일 walletId에 동일 transactionId로 요청합니다. (멱등 경합)

CI에는 포함하지 않으며, 로컬 실행 가이드만 제공합니다.

## 1. 사전 준비

- [k6](https://k6.io/docs/get-started/installation/)를 설치합니다.
- Docker(Postgres, Redis)가 필요합니다.

## 2. 실행 순서

1. 인프라를 기동합니다. 프로젝트 루트에서 아래 명령을 실행합니다.
   ```bash
   docker compose up -d
   ```

2. 애플리케이션을 실행합니다.
   ```bash
   ./gradlew bootRun
   ```

3. 초기 데이터는 Flyway 마이그레이션이 기동 시 자동 적용됩니다.
   - V1 : 스키마 생성
   - V2 : 초기 시드 (사용자 1 지갑 id 1 잔액 100,000 등)
   - V3 : 지갑 1 잔액 10,000,000으로 증가. (부하테스트용)
   - 별도 초기화 작업은 필요 없습니다.

4. k6를 실행합니다. 프로젝트 루트에서 아래 명령을 실행합니다.
   ```bash
   k6 run loadtest/k6/withdraw.js
   ```

   환경변수를 지정할 때는 다음 예를 참고합니다.
   ```bash
   BASE_URL=http://localhost:8080 WALLET_ID=1 OWNER_USER_ID=1 k6 run loadtest/k6/withdraw.js
   ```

## 3. 환경변수

| 변수 | 기본값 | 설명 |
|------|--------|------|
| BASE_URL | http://localhost:8080 | API 베이스 URL입니다. |
| WALLET_ID | 1 | 출금 대상 지갑 ID입니다. |
| OWNER_USER_ID | 1 | User-Id 헤더 값입니다. |
| SHARED_TX_ID | (자동) | Scenario B에서 사용할 공통 transactionId입니다. (선택) |

## 4. 시나리오·옵션

| 시나리오 | 구간 | 설정 | 설명 |
|----------|------|------|------|
| A | 0~30초 | 20 req/s | 매 요청마다 서로 다른 transactionId를 사용합니다. |
| B | 35~65초 | 20 req/s | 동일한 transactionId를 사용합니다. (멱등) |

- **Thresholds** : `http_req_failed` 5% 미만, `http_req_duration` p95 3000ms 미만입니다. 수정이 필요하면 `loadtest/k6/withdraw.js`의 `options`에서 변경합니다.

총 소요 시간은 약 1분입니다.
