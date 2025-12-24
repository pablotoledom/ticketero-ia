// =============================================================================
// TICKETERO - K6 Spike Test
// =============================================================================
// Usage: k6 run --vus 50 --duration 10s k6/spike-test.js
// =============================================================================

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// Custom metrics
const ticketsCreated = new Counter('tickets_created');
const ticketErrors = new Rate('ticket_errors');
const createLatency = new Trend('create_latency', true);

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const QUEUES = ['CAJA', 'PERSONAL', 'EMPRESAS', 'GERENCIA'];

// Spike test options
export const options = {
    stages: [
        { duration: '2s', target: 50 },   // Ramp up to 50 users
        { duration: '5s', target: 50 },   // Stay at 50 users
        { duration: '3s', target: 0 },    // Ramp down
    ],
    thresholds: {
        http_req_duration: ['p(95)<5000'],  // p95 < 5s (relaxed for spike)
        ticket_errors: ['rate<0.05'],        // < 5% errors (relaxed for spike)
        tickets_created: ['count>200'],      // > 200 tickets
    },
};

// Unique ID generator
function generateNationalId() {
    return Math.floor(40000000 + Math.random() * 10000000).toString();
}

function generatePhone() {
    return '+569' + Math.floor(10000000 + Math.random() * 90000000);
}

// Main test function
export default function () {
    const queue = QUEUES[Math.floor(Math.random() * QUEUES.length)];
    
    const payload = JSON.stringify({
        nationalId: generateNationalId(),
        telefono: generatePhone(),
        branchOffice: 'Sucursal Spike',
        queueType: queue,
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
        tags: { name: 'SpikeTest' },
    };

    const startTime = Date.now();
    const response = http.post(`${BASE_URL}/api/tickets`, payload, params);
    const duration = Date.now() - startTime;

    // Record metrics
    createLatency.add(duration);

    const success = check(response, {
        'status is 201 or 503': (r) => r.status === 201 || r.status === 503,
        'response time < 10s': (r) => r.timings.duration < 10000,
    });

    if (response.status === 201) {
        ticketsCreated.add(1);
    } else {
        ticketErrors.add(1);
    }

    // No sleep in spike test - maximum load
}

// Summary handler
export function handleSummary(data) {
    return {
        'stdout': textSummary(data),
        'results/spike-test-summary.json': JSON.stringify(data, null, 2),
    };
}

function textSummary(data) {
    const duration = data.metrics.http_req_duration;
    
    return `
═══════════════════════════════════════════════════════════════
  TICKETERO - SPIKE TEST RESULTS
═══════════════════════════════════════════════════════════════

  Total Requests:    ${data.metrics.http_reqs.values.count}
  Tickets Created:   ${data.metrics.tickets_created?.values.count || 0}
  Error Rate:        ${(data.metrics.ticket_errors?.values.rate * 100 || 0).toFixed(2)}%

  Latency:
    p50:  ${duration.values['p(50)'].toFixed(0)}ms
    p95:  ${duration.values['p(95)'].toFixed(0)}ms
    p99:  ${duration.values['p(99)'].toFixed(0)}ms
    max:  ${duration.values.max.toFixed(0)}ms

  Peak RPS:          ${data.metrics.http_reqs.values.rate.toFixed(1)}

═══════════════════════════════════════════════════════════════
`;
}