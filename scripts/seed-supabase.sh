#!/usr/bin/env bash
# Seed worker_plantation_assignments langsung ke Supabase, baca kredensial
# dari mysawit-shipment-service/.env (SPRING_DATASOURCE_URL/USERNAME/PASSWORD).
#
# Tujuan: bikin /api/shipments/available-supirs dan POST /api/shipments
# punya hasil saat profiling, bukan 400 "Mandor not assigned".
#
# Aman: cuma INSERT/UPSERT ke 1 tabel. Tidak ada DELETE/DROP.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$ROOT_DIR/.env"
SQL_FILE="$ROOT_DIR/src/main/resources/sql/seed-worker-assignments.sql"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "[!] $ENV_FILE tidak ditemukan" >&2
  exit 1
fi
if ! command -v psql >/dev/null 2>&1; then
  echo "[!] psql tidak ditemukan di PATH" >&2
  exit 1
fi

# Load .env tanpa export ke shell parent
get_env() {
  local key="$1"
  awk -F'=' -v k="$key" '
    /^[[:space:]]*#/ {next}
    $1 == k {
      sub(/^[^=]*=/, "")
      gsub(/^[ \t]+|[ \t]+$/, "")
      gsub(/^"|"$|^'\''|'\''$/, "")
      print
      exit
    }
  ' "$ENV_FILE"
}

JDBC_URL="$(get_env SPRING_DATASOURCE_URL)"
DB_USER="$(get_env SPRING_DATASOURCE_USERNAME)"
DB_PASS="$(get_env SPRING_DATASOURCE_PASSWORD)"

if [[ -z "$JDBC_URL" || -z "$DB_USER" || -z "$DB_PASS" ]]; then
  echo "[!] SPRING_DATASOURCE_URL/USERNAME/PASSWORD tidak lengkap di .env" >&2
  exit 1
fi

# JDBC -> host/port/db parsing
# Contoh: jdbc:postgresql://host:6543/postgres?sslmode=require&prepareThreshold=0
stripped="${JDBC_URL#jdbc:postgresql://}"
hostport_db="${stripped%%\?*}"
hostport="${hostport_db%%/*}"
db="${hostport_db#*/}"
host="${hostport%%:*}"
port="${hostport##*:}"
if [[ "$port" == "$host" ]]; then port="5432"; fi
sslmode="require"

echo "[*] Target: $host:$port/$db (user=$DB_USER)"
echo "[*] SQL   : $SQL_FILE"
echo

PGPASSWORD="$DB_PASS" psql \
  --host="$host" \
  --port="$port" \
  --username="$DB_USER" \
  --dbname="$db" \
  --set=sslmode="$sslmode" \
  -v ON_ERROR_STOP=1 \
  --no-password \
  -f "$SQL_FILE"

echo
echo "[OK] Seed selesai."
