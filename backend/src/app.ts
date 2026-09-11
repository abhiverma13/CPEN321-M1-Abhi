import express, { type Express, type NextFunction, type Request, type Response } from 'express';

export type TokenVerifier = (idToken: string) => Promise<void>;

export interface AppOptions {
  ownerFirstName: string;
  ownerLastName: string;
  serverPublicIp: string;
  verifyToken: TokenVerifier;
}

function clientIp(request: Request): string {
  const forwarded = request.header('x-forwarded-for')?.split(',')[0]?.trim();
  const address = forwarded || request.socket.remoteAddress || 'unknown';
  return address.replace(/^::ffff:/, '');
}

function serverTime(): string {
  const now = new Date();
  const offsetMinutes = -now.getTimezoneOffset();
  const offsetSign = offsetMinutes >= 0 ? '+' : '-';
  const offsetHours = String(Math.floor(Math.abs(offsetMinutes) / 60)).padStart(2, '0');
  const offsetRemainder = String(Math.abs(offsetMinutes) % 60).padStart(2, '0');
  const time = now.toLocaleTimeString('en-GB', { hour12: false });
  return `${time} GMT${offsetSign}${offsetHours}:${offsetRemainder}`;
}

function requireAuthentication(verifyToken: TokenVerifier) {
  return async (request: Request, response: Response, next: NextFunction): Promise<void> => {
    const authorization = request.header('authorization');
    const idToken = authorization?.startsWith('Bearer ') ? authorization.slice('Bearer '.length) : '';
    if (!idToken) {
      response.status(401).json({ error: 'Missing Google ID token' });
      return;
    }

    try {
      await verifyToken(idToken);
      next();
    } catch {
      response.status(401).json({ error: 'Invalid Google ID token' });
    }
  };
}

export function createApp(options: AppOptions): Express {
  const app = express();

  app.get('/health', (_req, res) => {
    res.json({ status: 'ok' });
  });

  const authenticate = requireAuthentication(options.verifyToken);

  app.get('/api/server/ip', authenticate, (request, response) => {
    response.json({ serverIp: options.serverPublicIp, clientIp: clientIp(request) });
  });

  app.get('/api/server/time', authenticate, (_request, response) => {
    response.json({ serverTime: serverTime() });
  });

  app.get('/api/server/owner', authenticate, (_request, response) => {
    response.json({ firstName: options.ownerFirstName, lastName: options.ownerLastName });
  });

  app.use((_req, res) => {
    res.status(404).json({ error: 'Not Found' });
  });

  return app;
}
