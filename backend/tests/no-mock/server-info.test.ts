import request from 'supertest';

import { createApp } from '../../src/app';

const app = createApp({
  ownerFirstName: 'Abhi',
  ownerLastName: 'Verma',
  serverPublicIp: '203.0.113.10',
  verifyToken: async (): Promise<void> => undefined,
});

const authorization = { Authorization: 'Bearer valid-test-token' };

// Interface GET /api/server/ip
describe('Unmocked: GET /api/server/ip', () => {
  test('returns the configured server IP and a client IP after authentication', async () => {
    const response = await request(app).get('/api/server/ip').set(authorization);

    expect(response.status).toBe(200);
    expect(response.body.serverIp).toBe('203.0.113.10');
    expect(typeof response.body.clientIp).toBe('string');
  });
});

// Interface GET /api/server/time
describe('Unmocked: GET /api/server/time', () => {
  test('returns a 24-hour time with its GMT offset after authentication', async () => {
    const response = await request(app).get('/api/server/time').set(authorization);

    expect(response.status).toBe(200);
    expect(response.body.serverTime).toMatch(/^\d{2}:\d{2}:\d{2} GMT[+-]\d{2}:\d{2}$/);
  });
});

// Interface GET /api/server/owner
describe('Unmocked: GET /api/server/owner', () => {
  test('returns the configured owner name after authentication', async () => {
    const response = await request(app).get('/api/server/owner').set(authorization);

    expect(response.status).toBe(200);
    expect(response.body).toEqual({ firstName: 'Abhi', lastName: 'Verma' });
  });
});

// Interface GET /api/server/owner
describe('Unmocked: GET /api/server/owner without a token', () => {
  test('rejects an unauthenticated request', async () => {
    const response = await request(app).get('/api/server/owner');

    expect(response.status).toBe(401);
    expect(response.body).toEqual({ error: 'Missing Google ID token' });
  });
});
