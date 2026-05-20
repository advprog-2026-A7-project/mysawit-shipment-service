import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 50 },  // Ramp up to 50 virtual users
    { duration: '1m', target: 50 },   // Hold at 50 virtual users
    { duration: '30s', target: 0 },   // Ramp down to 0
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% of requests should be below 500ms
    http_req_failed: ['rate<0.01'],   // Error rate should be less than 1%
  },
};

const BASE_URL = __ENV.API_URL || 'http://localhost:8084';
const JWT_TOKEN = __ENV.JWT_TOKEN || 'PUT_VALID_TOKEN_HERE';

export default function () {
  const params = {
    headers: {
      'Authorization': `Bearer ${JWT_TOKEN}`,
      'Content-Type': 'application/json',
    },
  };

  // 1. Load Testing: Simulate concurrent reads on API shipments (CQRS replica read model)
  const res = http.get(`${BASE_URL}/api/shipments`, params);
  
  check(res, {
    'status is 200': (r) => r.status === 200,
    'response is json': (r) => r.headers['Content-Type'] && r.headers['Content-Type'].includes('application/json'),
  });

  sleep(1);
}
