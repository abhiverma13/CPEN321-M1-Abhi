# CPEN 321 M1 App

## Prerequisites

- Docker Desktop running, with `docker version` and `docker compose version` working
- Android Studio, JDK 17+, and a Pixel 9 API 36 emulator named `Pixel_9`
- Node.js 22 and npm 10 for backend tests

## Local setup

Clone the repository and enter it:

```powershell
git clone https://github.com/abhiverma13/CPEN321-M1-Abhi.git
cd CPEN321-M1-Abhi
```

Create the backend configuration file:

```powershell
Copy-Item backend\.env.example backend\.env
```

Set `backend/.env` to:

```properties
PORT=3000
NODE_ENV=development
GOOGLE_CLIENT_ID=986175081923-12poichbpt3ej109488ist07e5omivqh.apps.googleusercontent.com
OWNER_FIRST_NAME=Abhi
OWNER_LAST_NAME=Verma
SERVER_PUBLIC_IP=127.0.0.1
COURSE_WEBSOCKET_URL=wss://8.229.22.124
TLS_CERT_PATH=
TLS_KEY_PATH=
```

Create `frontend/local.properties`. It must contain the Android SDK location and:

```properties
sdk.dir=C\:\\Users\\<username>\\AppData\\Local\\Android\\Sdk
API_BASE_URL=http://10.0.2.2:3000
GOOGLE_CLIENT_ID=986175081923-12poichbpt3ej109488ist07e5omivqh.apps.googleusercontent.com
```

`10.0.2.2` is the Android emulator alias for the development computer. Do not commit `.env`,
`local.properties`, keystores, or private keys.

## Run locally

From the repository root, start the backend:

```powershell
.\scripts\run-backend.ps1
```

Then build, install, and launch the Android app:

```powershell
.\scripts\run-frontend.ps1
```

Stop the local backend with:

```powershell
docker compose down
```

## Tests

Run backend interface tests from the repository root:

```powershell
.\scripts\run-backend-interface-tests.ps1
```

For a local debug build, Google Sign-In requires the Android OAuth client to include the SHA-1 of
the debug key used to sign that build.
