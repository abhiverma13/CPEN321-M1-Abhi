import 'dotenv/config';

const rawPort = process.env.PORT;
const port =
  rawPort === undefined || rawPort === ''
    ? 3000
    : Number.parseInt(rawPort, 10);

if (Number.isNaN(port) || port < 1 || port > 65535) {
  throw new Error(`Invalid PORT: ${rawPort}`);
}

export const env = {
  port,
  ownerFirstName: process.env.OWNER_FIRST_NAME?.trim() || 'Abhi',
  ownerLastName: process.env.OWNER_LAST_NAME?.trim() || 'Verma',
  serverPublicIp: process.env.SERVER_PUBLIC_IP?.trim() || '127.0.0.1',
  googleClientId: process.env.GOOGLE_CLIENT_ID?.trim() || '',
  courseWebSocketUrl: process.env.COURSE_WEBSOCKET_URL?.trim() || 'wss://8.229.22.124',
  tlsCertPath: process.env.TLS_CERT_PATH?.trim() || '',
  tlsKeyPath: process.env.TLS_KEY_PATH?.trim() || '',
} as const;
