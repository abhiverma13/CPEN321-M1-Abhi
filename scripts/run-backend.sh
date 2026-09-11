#!/usr/bin/env bash
#
# Assumes config (e.g., backend/.env) is already setup, starts docker compose.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

info()  { printf '\033[34m==>\033[0m %s\n' "$*"; }
die()   { printf '\033[31mERROR:\033[0m %s\n' "$*" >&2; exit 1; }

# ---------------------------------------------------------------------------
# Prerequisites
# ---------------------------------------------------------------------------
command -v docker >/dev/null 2>&1 || die "Docker not found."
docker info >/dev/null 2>&1       || die "Docker is not running."
command -v curl >/dev/null 2>&1    || die "curl not found."

[[ -f backend/.env ]] || die "Missing backend/.env — follow the student setup guide first."

BACKEND_PORT="$(sed -n 's/^PORT=//p' backend/.env | head -1 | tr -d '[:space:]' || true)"
BACKEND_PORT="${BACKEND_PORT:-3000}"

# docker-compose.yml reads PORT while it expands the published-port mapping.
# An env_file only configures the container; it does not set Compose variables.
export PORT="$BACKEND_PORT"

TLS_CERT_CONFIGURED="$(grep -E '^TLS_CERT_PATH=.+$' backend/.env | head -1 || true)"
TLS_KEY_CONFIGURED="$(grep -E '^TLS_KEY_PATH=.+$' backend/.env | head -1 || true)"
if [[ -n "$TLS_CERT_CONFIGURED" && -n "$TLS_KEY_CONFIGURED" ]]; then
  HEALTH_SCHEME='https'
  CURL_TLS_ARGS=(-k)
else
  HEALTH_SCHEME='http'
  CURL_TLS_ARGS=()
fi

BACKEND_HEALTH_URL="${BACKEND_HEALTH_URL:-${HEALTH_SCHEME}://localhost:${BACKEND_PORT}/health}"

# ---------------------------------------------------------------------------
# Backend
# ---------------------------------------------------------------------------

info "Starting backend (docker compose up --build -d)..."
docker compose up --build -d

info "Waiting for $BACKEND_HEALTH_URL ..."
for _ in $(seq 1 120); do
  curl -sf "${CURL_TLS_ARGS[@]}" "$BACKEND_HEALTH_URL" >/dev/null 2>&1 && break
  sleep 1
done
curl -sf "${CURL_TLS_ARGS[@]}" "$BACKEND_HEALTH_URL" >/dev/null 2>&1 || die "Backend not healthy. Try: docker compose logs backend"

echo
info "Backend is up. Stop with: docker compose down"
