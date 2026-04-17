#!/usr/bin/env bash
set -Eeuo pipefail

APP_NAME="${APP_NAME:-mysawit-shipment}"
CONTAINER_NAME="${CONTAINER_NAME:-mysawit-shipment}"
APP_HOME="${APP_HOME:?APP_HOME is required}"
RELEASE_DIR="${RELEASE_DIR:?RELEASE_DIR is required}"
IMAGE_TAG="${IMAGE_TAG:?IMAGE_TAG is required}"
APP_PORT="${APP_PORT:-8084}"
HOST_PORT="${HOST_PORT:-8084}"
HEALTH_PATH="${HEALTH_PATH:-/actuator/health/readiness}"
ENV_SOURCE_FILE="${ENV_SOURCE_FILE:-$RELEASE_DIR/.env.deploy}"
SHARED_DIR="${APP_HOME}/shared"
RELEASES_DIR="${APP_HOME}/releases"
CURRENT_LINK="${APP_HOME}/current"
RUNTIME_ENV_FILE="${SHARED_DIR}/app.env"
PREVIOUS_ENV_FILE="${SHARED_DIR}/app.env.previous"
DEPLOY_LOG="${DEPLOY_LOG:-${APP_HOME}/deploy.log}"
NEW_IMAGE="${APP_NAME}:${IMAGE_TAG}"
PREVIOUS_IMAGE_ID=""
ROLLBACK_ATTEMPTED=0

mkdir -p "$APP_HOME" "$SHARED_DIR" "$RELEASES_DIR"
touch "$DEPLOY_LOG"
exec > >(tee -a "$DEPLOY_LOG") 2>&1

log() {
  printf '[%s] %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$*"
}

docker_cmd() {
  if docker info >/dev/null 2>&1; then
    docker "$@"
  else
    sudo docker "$@"
  fi
}

require_file() {
  local path="$1"
  if [ ! -f "$path" ]; then
    log "Required file missing: $path"
    exit 1
  fi
}

env_value() {
  local key="$1"
  awk -F= -v key="$key" '$1 == key {sub(/^[^=]*=/, "", $0); print $0}' "$ENV_SOURCE_FILE" | tail -n 1
}

extract_db_host() {
  local jdbc_url="$1"
  local authority="${jdbc_url#jdbc:postgresql://}"
  authority="${authority%%/*}"
  authority="${authority%%\?*}"
  authority="${authority##*@}"

  if [[ "$authority" == \[*\]:* ]]; then
    local bracketed="${authority#[}"
    printf '%s\n' "${bracketed%%\]:*}"
    return
  fi

  printf '%s\n' "${authority%%:*}"
}

print_ipv6_state() {
  log "EC2 IPv6 addresses"
  ip -6 addr show scope global || true
  log "EC2 IPv6 routes"
  ip -6 route show || true
}

resolve_ipv6_records() {
  local host="$1"
  python3 - "$host" <<'PY'
import socket
import sys

host = sys.argv[1]
records = sorted({info[4][0] for info in socket.getaddrinfo(host, None, socket.AF_INET6, socket.SOCK_STREAM)})
if not records:
    raise SystemExit(1)
for record in records:
    print(record)
PY
}

check_tcp_ipv6() {
  local host="$1"
  local port="$2"
  python3 - "$host" "$port" <<'PY'
import socket
import sys

host = sys.argv[1]
port = int(sys.argv[2])

last_error = None
for info in socket.getaddrinfo(host, port, socket.AF_INET6, socket.SOCK_STREAM):
    family, socktype, proto, _, sockaddr = info
    sock = socket.socket(family, socktype, proto)
    sock.settimeout(5)
    try:
        sock.connect(sockaddr)
        print(f"Connected to [{sockaddr[0]}]:{sockaddr[1]}")
        sock.close()
        raise SystemExit(0)
    except OSError as exc:
        last_error = exc
    finally:
        sock.close()

raise SystemExit(f"IPv6 TCP connect failed: {last_error}")
PY
}

container_status() {
  docker_cmd ps -a --filter "name=^/${CONTAINER_NAME}$" || true
  docker_cmd inspect \
    --format 'status={{.State.Status}} health={{if .State.Health}}{{.State.Health.Status}}{{else}}n/a{{end}} image={{.Image}}' \
    "$CONTAINER_NAME" 2>/dev/null || true
}

recent_container_logs() {
  docker_cmd logs --tail 200 "$CONTAINER_NAME" 2>/dev/null || true
}

rollback() {
  if [ "$ROLLBACK_ATTEMPTED" -eq 1 ]; then
    return
  fi

  if [ -z "$PREVIOUS_IMAGE_ID" ]; then
    log "Rollback skipped: no previous image found"
    return
  fi

  ROLLBACK_ATTEMPTED=1
  log "Attempting rollback using image ${PREVIOUS_IMAGE_ID}"

  docker_cmd rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
  if [ -f "$PREVIOUS_ENV_FILE" ]; then
    cp "$PREVIOUS_ENV_FILE" "$RUNTIME_ENV_FILE"
  fi

  docker_cmd run -d \
    --name "$CONTAINER_NAME" \
    --network host \
    --env-file "$RUNTIME_ENV_FILE" \
    --restart unless-stopped \
    --log-opt max-size=10m \
    --log-opt max-file=5 \
    "$PREVIOUS_IMAGE_ID" >/dev/null

  log "Rollback container started"
}

trap 'rc=$?; log "Deployment failed with exit code ${rc}"; container_status; recent_container_logs; rollback; exit "$rc"' ERR

if [ "$HOST_PORT" != "$APP_PORT" ]; then
  log "HOST_PORT (${HOST_PORT}) must match APP_PORT (${APP_PORT}) when using --network host"
  exit 1
fi

require_file "$ENV_SOURCE_FILE"
require_file "$RELEASE_DIR/Dockerfile"
require_file "$RELEASE_DIR/build/libs/app.jar"

PREVIOUS_IMAGE_ID="$(docker_cmd inspect --format '{{.Image}}' "$CONTAINER_NAME" 2>/dev/null || docker_cmd image inspect --format '{{.Id}}' "${APP_NAME}:latest" 2>/dev/null || true)"

install -m 600 "$ENV_SOURCE_FILE" "$RUNTIME_ENV_FILE.new"
if [ -f "$RUNTIME_ENV_FILE" ]; then
  cp "$RUNTIME_ENV_FILE" "$PREVIOUS_ENV_FILE"
fi
mv "$RUNTIME_ENV_FILE.new" "$RUNTIME_ENV_FILE"

DB_JDBC_URL="$(env_value SPRING_DATASOURCE_URL)"
DB_HOST="$(extract_db_host "$DB_JDBC_URL")"

if [ -z "$DB_HOST" ]; then
  log "Could not parse DB host from SPRING_DATASOURCE_URL"
  exit 1
fi

log "Deploying ${APP_NAME}:${IMAGE_TAG} from ${RELEASE_DIR}"
log "Using Docker host networking on port ${APP_PORT}"
log "Derived Supabase DB host: ${DB_HOST}"

print_ipv6_state

log "Resolving IPv6 records for ${DB_HOST}"
resolve_ipv6_records "$DB_HOST"

log "Checking IPv6 TCP connectivity to ${DB_HOST}:5432"
check_tcp_ipv6 "$DB_HOST" 5432

docker_cmd build --pull \
  --label "com.mysawit.service=${APP_NAME}" \
  --label "com.mysawit.release=${IMAGE_TAG}" \
  -t "$NEW_IMAGE" \
  -t "${APP_NAME}:latest" \
  "$RELEASE_DIR"

docker_cmd rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true

docker_cmd run -d \
  --name "$CONTAINER_NAME" \
  --network host \
  --env-file "$RUNTIME_ENV_FILE" \
  --restart unless-stopped \
  --log-opt max-size=10m \
  --log-opt max-file=5 \
  "$NEW_IMAGE" >/dev/null

log "Waiting for application readiness on http://127.0.0.1:${APP_PORT}${HEALTH_PATH}"
for attempt in $(seq 1 36); do
  if curl -fsS "http://127.0.0.1:${APP_PORT}${HEALTH_PATH}"; then
    log "Application is ready"
    break
  fi

  if [ "$attempt" -eq 36 ]; then
    log "Application readiness timed out"
    exit 1
  fi

  container_status
  sleep 5
done

ln -sfn "$RELEASE_DIR" "$CURRENT_LINK"

log "Listening sockets for port ${APP_PORT}"
ss -ltn "( sport = :${APP_PORT} )" || true
container_status

CURRENT_IMAGE_ID="$(docker_cmd image inspect --format '{{.Id}}' "$NEW_IMAGE")"
while IFS= read -r image_id; do
  [ -z "$image_id" ] && continue
  if [ "$image_id" != "$CURRENT_IMAGE_ID" ] && [ "$image_id" != "$PREVIOUS_IMAGE_ID" ]; then
    docker_cmd rmi -f "$image_id" >/dev/null 2>&1 || true
  fi
done < <(docker_cmd image ls "$APP_NAME" --format '{{.ID}}' | sort -u)

docker_cmd image prune -f >/dev/null 2>&1 || true
find "$RELEASES_DIR" -mindepth 1 -maxdepth 1 -type d | sort | head -n -5 | xargs -r rm -rf >/dev/null 2>&1 || true

log "Deployment finished successfully"
