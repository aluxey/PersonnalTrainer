#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
ENV_FILE="$ROOT_DIR/.env"
PAYLOAD_FILE="${1:-$ROOT_DIR/examples/api/health-connect-daily.json}"

if [ ! -f "$ENV_FILE" ]; then
  echo "Missing .env at $ENV_FILE" >&2
  exit 1
fi

PORT="$(awk -F= '/^PORT=/{print $2; exit}' "$ENV_FILE")"
HOST="$(awk -F= '/^HOST=/{print $2; exit}' "$ENV_FILE")"
API_KEY="$(awk -F= '/^INGEST_API_KEY=/{print $2; exit}' "$ENV_FILE")"

PORT="${PORT:-8787}"
HOST="${HOST:-127.0.0.1}"

if [ -z "$API_KEY" ]; then
  echo "Missing INGEST_API_KEY in .env" >&2
  exit 1
fi

curl -X POST "http://$HOST:$PORT/api/ingest/health-connect" \
  -H "content-type: application/json" \
  -H "x-api-key: $API_KEY" \
  --data @"$PAYLOAD_FILE"
