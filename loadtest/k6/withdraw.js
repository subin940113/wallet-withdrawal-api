import http from 'k6/http';
import { check, sleep } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const WALLET_ID = __ENV.WALLET_ID || '1';
const OWNER_USER_ID = __ENV.OWNER_USER_ID || '1';
const EXTRA_HEADERS_JSON = __ENV.EXTRA_HEADERS_JSON || '';
const AMOUNT = 10000;

function buildHeaders() {
  const headers = {
    'Content-Type': 'application/json',
    'User-Id': String(OWNER_USER_ID),
  };
  if (EXTRA_HEADERS_JSON) {
    try {
      const extra = JSON.parse(EXTRA_HEADERS_JSON);
      Object.assign(headers, extra);
    } catch (_) {}
  }
  return headers;
}

export const options = {
  scenarios: {
    scenario_a_hot_key: {
      executor: 'constant-arrival-rate',
      rate: 20,
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 10,
      maxVUs: 50,
      startTime: '0s',
      exec: 'scenarioA',
    },
    scenario_b_idempotency: {
      executor: 'constant-arrival-rate',
      rate: 20,
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 10,
      maxVUs: 50,
      startTime: '35s',
      exec: 'scenarioB',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<3000'],
  },
};

export function scenarioA() {
  const url = `${BASE_URL}/api/wallets/${WALLET_ID}/withdraw`;
  const payload = JSON.stringify({
    transaction_id: uuidv4(),
    amount: AMOUNT,
  });
  const params = { headers: buildHeaders() };
  const res = http.post(url, payload, params);
  check(res, { 'status 200 or 4xx': (r) => r.status === 200 || (r.status >= 400 && r.status < 500) });
  sleep(0.1);
}

const SHARED_TX_ID = __ENV.SHARED_TX_ID || `idempotency-${Date.now()}`;

export function scenarioB() {
  const url = `${BASE_URL}/api/wallets/${WALLET_ID}/withdraw`;
  const payload = JSON.stringify({
    transaction_id: SHARED_TX_ID,
    amount: AMOUNT,
  });
  const params = { headers: buildHeaders() };
  const res = http.post(url, payload, params);
  check(res, { 'status 200 or 4xx': (r) => r.status === 200 || (r.status >= 400 && r.status < 500) });
  sleep(0.1);
}
