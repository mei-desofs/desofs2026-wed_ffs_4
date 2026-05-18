#!/usr/bin/env bash
set -euo pipefail

MODE="${1:-all}"
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"

if command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_CMD="docker-compose"
elif command -v docker >/dev/null 2>&1; then
  COMPOSE_CMD="docker compose"
else
  echo "[ERROR] Docker Compose not found." >&2
  exit 1
fi

start_db() {
  echo "[1/3] Starting Postgres..."
  (cd "$ROOT_DIR" && $COMPOSE_CMD up -d)
  echo "[OK] Postgres started."
}

reset_db() {
  echo "[1/3] Resetting Postgres data..."
  (cd "$ROOT_DIR" && $COMPOSE_CMD down -v --remove-orphans >/dev/null 2>&1 || true)
  echo "[OK] Postgres data reset."
}

wait_for_db() {
  echo "[2/3] Waiting for Postgres to be ready..."
  local attempts=30
  while [[ $attempts -gt 0 ]]; do
    if (cd "$ROOT_DIR" && $COMPOSE_CMD exec -T db pg_isready -U postgres -d desofs) >/dev/null 2>&1; then
      echo "[OK] Postgres is ready."
      return 0
    fi
    sleep 2
    attempts=$((attempts - 1))
  done

  echo "[ERROR] Postgres did not become ready in time." >&2
  exit 1
}

start_api() {
  echo "[3/3] Starting Spring Boot..."
  cd "$ROOT_DIR"
  mvn spring-boot:run
}

show_smoke_commands() {
  cat <<'EOF'

[3/3] Quick test (open another terminal):

# Login with admin
curl -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" -d '{"email":"admin@example.com","password":"password123"}'

# or login with regular user
curl -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" -d '{"email":"user@example.com","password":"password123"}'

# Set TOKEN from response above
TOKEN="YOUR_TOKEN_HERE"

# Create project
curl -X POST http://localhost:8080/api/projects -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -d '{"name":"Test Project","description":"Demo"}'

# List projects
curl -X GET http://localhost:8080/api/projects -H "Authorization: Bearer $TOKEN"
EOF
}

case "$MODE" in
  all)
    reset_db
    start_db
    wait_for_db
    show_smoke_commands
    start_api
    ;;
  db)
    start_db
    ;;
  api)
    start_api
    ;;
  smoke)
    show_smoke_commands
    ;;
  *)
    echo "Usage: ./run.sh [all|db|api|smoke]"
    exit 1
    ;;
esac
