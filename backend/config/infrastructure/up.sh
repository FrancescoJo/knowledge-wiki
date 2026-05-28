#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

ENV_FILE="$SCRIPT_DIR/.env"
if [[ ! -f "$ENV_FILE" ]]; then
    echo "Error: $ENV_FILE not found." >&2
    echo "Copy $SCRIPT_DIR/.env.template to $SCRIPT_DIR/.env and fill in the values." >&2
    exit 1
fi

docker compose --env-file "$ENV_FILE" -f "$SCRIPT_DIR/docker-compose.yml" up -d
