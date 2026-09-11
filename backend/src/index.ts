import { readFileSync } from 'node:fs';
import { createServer as createHttpServer } from 'node:http';
import { createServer as createHttpsServer } from 'node:https';

import { OAuth2Client } from 'google-auth-library';
import { WebSocket, WebSocketServer } from 'ws';

import { createApp } from './app';
import { env } from './config/env';

const googleClient = new OAuth2Client();
const app = createApp({
  ownerFirstName: env.ownerFirstName,
  ownerLastName: env.ownerLastName,
  serverPublicIp: env.serverPublicIp,
  verifyToken: async (idToken) => {
    if (!env.googleClientId) {
      throw new Error('GOOGLE_CLIENT_ID is not configured');
    }
    await googleClient.verifyIdToken({ idToken, audience: env.googleClientId });
  },
});

const hasTlsConfiguration = Boolean(env.tlsCertPath && env.tlsKeyPath);
const server = hasTlsConfiguration
  ? createHttpsServer(
      {
        cert: readFileSync(env.tlsCertPath),
        key: readFileSync(env.tlsKeyPath),
      },
      app,
    )
  : createHttpServer(app);

const pixelServer = new WebSocketServer({ server, path: '/ws/pixels' });
pixelServer.on('connection', (client) => {
  const courseStream = new WebSocket(env.courseWebSocketUrl);

  courseStream.on('message', (message, isBinary) => {
    if (client.readyState === WebSocket.OPEN) {
      client.send(message, { binary: isBinary });
    }
  });

  courseStream.on('error', () => {
    if (client.readyState === WebSocket.OPEN) {
      client.close(1011, 'Course pixel stream is unavailable');
    }
  });

  client.on('close', () => {
    courseStream.close();
  });
});

server.listen(env.port, () => {
  console.log(`Server listening on ${hasTlsConfiguration ? 'HTTPS' : 'HTTP'} port ${env.port}`);
});

for (const signal of ['SIGINT', 'SIGTERM'] as const) {
  process.on(signal, () => {
    server.close(() => {
      process.exit(0);
    });
  });
}
