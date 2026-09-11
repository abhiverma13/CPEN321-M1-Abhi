# CPEN 321 M1 App

Native Kotlin/Jetpack Compose Android app with a Node.js/TypeScript backend.

## Prerequisites

- Docker with Docker Compose v2
- Android Studio, JDK 17+, and a Pixel 9 API 36 AVD named `Pixel_9`
- Node.js 22 and npm 10 for backend tests
- Google OAuth Web client ID; Android OAuth clients for debug and release signing certificates

## Configuration

Do not commit `backend/.env`, `frontend/local.properties`, keystores, or TLS private keys.

Create `backend/.env` from `backend/.env.example`. For local development:

```properties
PORT=3000
NODE_ENV=development
GOOGLE_CLIENT_ID=986175081923-12poichbpt3ej109488ist07e5omivqh.apps.googleusercontent.com
OWNER_FIRST_NAME=<your first name>
OWNER_LAST_NAME=<your last name>
SERVER_PUBLIC_IP=127.0.0.1
COURSE_WEBSOCKET_URL=wss://8.229.22.124
TLS_CERT_PATH=
TLS_KEY_PATH=
```

Set these values in `frontend/local.properties` in addition to Android Studio's `sdk.dir`:

```properties
API_BASE_URL=http://10.0.2.2:3000
GOOGLE_CLIENT_ID=986175081923-12poichbpt3ej109488ist07e5omivqh.apps.googleusercontent.com
```

`10.0.2.2` is the Android emulator alias for the development computer.

## Run locally

From the repository root, start the backend:

```powershell
.\scripts\run-backend.ps1
```

Then build, install, and launch the Android app:

```powershell
.\scripts\run-frontend.ps1
```

Stop local containers with `docker compose down`.

## Tests

Run backend interface tests from the repository root:

```powershell
.\scripts\run-backend-interface-tests.ps1
```

The final M1 check is manual: test all three buttons in the signed release APK against the deployed
backend.

## Google Cloud deployment

The production backend is deployed at `https://35.230.102.241` on port 443. Its VM-only
`backend/.env` uses:

```properties
PORT=443
NODE_ENV=production
GOOGLE_CLIENT_ID=986175081923-12poichbpt3ej109488ist07e5omivqh.apps.googleusercontent.com
SERVER_PUBLIC_IP=35.230.102.241
TLS_CERT_PATH=/certs/server.crt
TLS_KEY_PATH=/certs/server.key
```

The VM certificate and private key are stored at `backend/certs/server.crt` and
`backend/certs/server.key`. The public certificate committed at
`frontend/app/src/main/res/raw/m1_backend_certificate.pem` is the trust anchor for the Android
app. If the server certificate changes, replace that public resource and rebuild the APK.

On the Ubuntu VM, create the certificate and start the backend with:

```bash
mkdir -p backend/certs
openssl req -x509 -newkey rsa:2048 -sha256 -nodes -days 365 \
  -keyout backend/certs/server.key -out backend/certs/server.crt \
  -subj "/CN=35.230.102.241" -addext "subjectAltName=IP:35.230.102.241"
chmod 600 backend/certs/server.key
./scripts/run-backend.sh
```

For the release APK, set `API_BASE_URL=https://35.230.102.241` in `frontend/local.properties` and
register the release signing-key SHA-1 in Google OAuth before building the APK.
