import request from 'supertest';

import { createApp } from '../../src/app';

// Interface GET /api/server/owner
describe('Mocked: GET /api/server/owner', () => {
  test('returns 401 when Google token verification fails', async () => {
    const verifyToken = jest.fn(async (): Promise<void> => {
      throw new Error('Simulated Google token verification failure');
    });
    const app = createApp({
      ownerFirstName: 'Abhi',
      ownerLastName: 'Verma',
      serverPublicIp: '203.0.113.10',
      verifyToken,
    });

    const response = await request(app)
      .get('/api/server/owner')
      .set('Authorization', 'Bearer expired-token');

    expect(response.status).toBe(401);
    expect(response.body).toEqual({ error: 'Invalid Google ID token' });
    expect(verifyToken).toHaveBeenCalledWith('expired-token');
  });
});
