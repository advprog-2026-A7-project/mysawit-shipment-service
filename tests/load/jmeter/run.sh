#!/usr/bin/env bash
# Profiling shipment-service via JMeter (1000 GET requests).
#
# Usage:
#   ./run.sh                          # run dengan default (BASE_URL=http://localhost:8084)
#   BASE_URL=http://1.2.3.4:8084 ./run.sh
#   THREADS=100 LOOPS_PER_ENDPOINT=2 ./run.sh
#
# Pre-req:
#   - JMeter terinstall (brew install jmeter)
#   - Node.js terinstall
#   - JWT_SECRET di mysawit-shipment-service/.env (atau export JWT_SECRET sendiri)
#   - shipment-service running (lokal atau remote)
#
# Output: results/<timestamp>/{jmeter.log, results.jtl, report/index.html}

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

BASE_URL="${BASE_URL:-http://localhost:8084}"
THREADS="${THREADS:-50}"
LOOPS_PER_ENDPOINT="${LOOPS_PER_ENDPOINT:-5}"   # 4 endpoint x 50 thread x 5 loop = 1000 req
RAMPUP_SECONDS="${RAMPUP_SECONDS:-10}"
SHIPMENT_ID="${SHIPMENT_ID:-11111111-1111-1111-1111-111111111111}"

if ! command -v jmeter >/dev/null 2>&1; then
  echo "[!] jmeter tidak ditemukan di PATH. Install dulu: brew install jmeter" >&2
  exit 1
fi

if ! command -v node >/dev/null 2>&1; then
  echo "[!] node tidak ditemukan. Install dulu: brew install node" >&2
  exit 1
fi

echo "[*] Generating JWT (HS256) dari JWT_SECRET..."
TOKEN_OUT="$(node "$SCRIPT_DIR/generate-jwt.mjs")"
JWT_MANDOR="$(echo "$TOKEN_OUT" | awk -F'=' '/^JWT_MANDOR=/{print $2}')"
JWT_SUPIR="$(echo  "$TOKEN_OUT" | awk -F'=' '/^JWT_SUPIR=/{print $2}')"
JWT_ADMIN="$(echo  "$TOKEN_OUT" | awk -F'=' '/^JWT_ADMIN=/{print $2}')"

if [[ -z "$JWT_SUPIR" || -z "$JWT_MANDOR" || -z "$JWT_ADMIN" ]]; then
  echo "[!] Gagal generate JWT" >&2
  exit 1
fi

echo "[*] Smoke test ${BASE_URL}/api/shipments/health ..."
if ! curl -fsS -m 5 "${BASE_URL}/api/shipments/health" >/dev/null; then
  echo "[!] Service tidak respon di ${BASE_URL}. Pastikan shipment-service jalan." >&2
  exit 1
fi

TS="$(date +%Y%m%d-%H%M%S)"
OUT_DIR="$SCRIPT_DIR/results/$TS"
mkdir -p "$OUT_DIR"
JTL="$OUT_DIR/results.jtl"
LOG="$OUT_DIR/jmeter.log"
REPORT="$OUT_DIR/report"

echo "[*] BASE_URL=$BASE_URL"
echo "[*] THREADS=$THREADS  LOOPS_PER_ENDPOINT=$LOOPS_PER_ENDPOINT  RAMPUP=${RAMPUP_SECONDS}s"
echo "[*] Total target: $(( 4 * THREADS * LOOPS_PER_ENDPOINT )) request"
echo "[*] Output: $OUT_DIR"
echo

jmeter -n \
  -t "$SCRIPT_DIR/shipment-1000req.jmx" \
  -l "$JTL" \
  -j "$LOG" \
  -e -o "$REPORT" \
  -JBASE_URL="$BASE_URL" \
  -JJWT_SUPIR="$JWT_SUPIR" \
  -JJWT_MANDOR="$JWT_MANDOR" \
  -JJWT_ADMIN="$JWT_ADMIN" \
  -JSHIPMENT_ID="$SHIPMENT_ID" \
  -JTHREADS="$THREADS" \
  -JLOOPS_PER_ENDPOINT="$LOOPS_PER_ENDPOINT" \
  -JRAMPUP_SECONDS="$RAMPUP_SECONDS"

echo
echo "[OK] Selesai. Buka HTML report:"
echo "     open \"$REPORT/index.html\""
