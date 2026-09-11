# CPEN 321 M1 App

A native Kotlin/Jetpack Compose Android app and Node.js/TypeScript backend for CPEN 321 M1.
The app has three independent features: Google sign-in plus server information, relayed live
pixel updates, and a countdown timer that reveals current weather.

## Prerequisites

- Node.js 22 and npm 10 for backend tests.
- Docker Desktop (Windows) or Docker Engine with Docker Compose v2 (Linux).
- Android Studio, JDK 17+, and a Pixel 9 API 36 emulator named `Pixel_9`.
- A Google OAuth **Web client ID** for token verification. An Android OAuth client matching the
  application ID and APK signing certificate is also required for Google sign-in.

## Configuration (not committed)

Copy `backend/.env.example` to `backend/.env` and configure it. For local development:

```properties
PORT=3000
NODE_ENV=development
GOOGLE_CLIENT_ID=<Google Web OAuth client ID>
OWNER_FIRST_NAME=<your first name>
OWNER_LAST_NAME=<your last name>
SERVER_PUBLIC_IP=127.0.0.1
COURSE_WEBSOCKET_URL=wss://8.229.22.124
TLS_CERT_PATH=
TLS_KEY_PATH=
```

Android Studio creates `frontend/local.properties` with `sdk.dir`. Add these project settings to
that same ignored file:

```properties
API_BASE_URL=http://10.0.2.2:3000
GOOGLE_CLIENT_ID=<Google Web OAuth client ID>
```

`10.0.2.2` is the Android emulator alias for the development computer. Never commit either
configuration file, signing keys, or TLS private keys.

## Run locally

From the repository root, start the backend first:

```powershell
.\scripts\run-backend.ps1
```

On macOS/Linux, use:

```bash
./scripts/run-backend.sh
```

The local health endpoint is `http://localhost:3000/health`. Stop the service with:

```powershell
docker compose down
```

Then build, install, and launch the Android app:

```powershell
.\scripts\run-frontend.ps1
```

The home screen should report a healthy backend. Verify each button independently:

1. **Login + Server:** sign in with Google and view the logged-in user, server IP, client IP,
   server time, client time, and developer name.
2. **Live Updates:** watch the 16 by 16 grid receive pixel updates through this backend.
3. **Timer:** enter minutes, seconds, and a city; when the timer finishes, current weather appears.

## Automated checks

Run these from the repository root:

```powershell
Push-Location backend
npm run typecheck
Pop-Location

.\scripts\run-backend-interface-tests.ps1

Push-Location frontend
.\gradlew.bat :app:compileDebugKotlin --no-daemon
Pop-Location
```

The frontend instrumented-test launcher requires a running emulator and an already signed-in app:

```powershell
.\scripts\run-frontend-e2e-tests.ps1
```

The NFR launchers are present for the course template but no NFR tests have been added for M1.

## Google Cloud deployment

The production VM uses the reserved external IPv4 address `35.230.102.241`. The VM must allow
incoming TCP 443. On the VM, create `backend/.env` using the same Google client ID and owner name,
with these production-specific values:

```properties
PORT=443
NODE_ENV=production
GOOGLE_CLIENT_ID=<Google Web OAuth client ID>
OWNER_FIRST_NAME=<your first name>
OWNER_LAST_NAME=<your last name>
SERVER_PUBLIC_IP=35.230.102.241
COURSE_WEBSOCKET_URL=wss://8.229.22.124
TLS_CERT_PATH=/certs/server.crt
TLS_KEY_PATH=/certs/server.key
```

Place the certificate and private key at `backend/certs/server.crt` and
`backend/certs/server.key`; that directory is ignored by Git and mounted read-only in the
container. With both TLS paths configured, `run-backend.sh` exports port 443 for Docker Compose,
starts HTTPS, and checks the local HTTPS health endpoint using `curl -k` so a self-signed
certificate can be used.

Before creating the final APK, set `API_BASE_URL=https://35.230.102.241` in
`frontend/local.properties` and add the SHA-1 fingerprint of the release signing key to the Android
OAuth client. The public self-signed certificate is already committed at
`frontend/app/src/main/res/raw/m1_backend_certificate.pem` and is trusted through the app's network
security configuration. If the VM certificate is replaced, replace this public certificate resource
and rebuild the APK as well.

## Submission preflight

Before submission, test the release APK on a Pixel 9 API 36 emulator against the deployed HTTPS
server. Perform one clean-clone test: clone the repository to a new directory, create the two
ignored configuration files, run the backend and frontend scripts, and verify all three features.
Keep the cloud backend running through grading.
