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

start_api() {
  echo "[2/3] Starting Spring Boot..."
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
    start_db
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
