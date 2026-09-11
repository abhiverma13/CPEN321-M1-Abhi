import request from 'supertest';

import { createApp } from '../../src/app';

const appOptions = {
  ownerFirstName: 'Abhi',
  ownerLastName: 'Verma',
  serverPublicIp: '203.0.113.10',
  verifyToken: async (): Promise<void> => undefined,
};

// Interface GET /health
describe('Unmocked: GET /health', () => {
  // Input: GET request to /health
  // Expected status code: 200
  // Expected behavior: service reports healthy status
  // Expected output: { status: "ok" }
  test('Healthy service', async () => {
    const response = await request(createApp(appOptions)).get('/health');

    expect(response.status).toBe(200);
    expect(response.body).toEqual({ status: 'ok' });
  });
});

// Interface GET /does-not-exist
describe('Unmocked: GET /does-not-exist', () => {
  // Input: GET request to an unregistered route
  // Expected status code: 404
  // Expected behavior: request is rejected; no state is changed
  // Expected output: { error: "Not Found" }
  test('Unregistered route', async () => {
    const response = await request(createApp(appOptions)).get('/does-not-exist');

    expect(response.status).toBe(404);
    expect(response.body).toEqual({ error: 'Not Found' });
  });
});
