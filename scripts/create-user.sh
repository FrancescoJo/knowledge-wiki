#!/usr/bin/env bash
# create-user.sh
#
# $Since: 2026-05-26T00:00:00Z
#
# Registers the first user account via the bootstrap endpoint.
# The endpoint is localhost-only and only succeeds when no users exist yet.
#
# Usage:
#   ./scripts/create-user.sh [BASE_URL]
#
# Arguments:
#   BASE_URL  Base URL of the running backend (default: http://localhost:8080)
#
# Requirements:
#   curl, python3

set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
ENDPOINT="${BASE_URL}/api/v1/bootstrap/users"

read -r -p "Email: " email
read -r -s -p "Password: " password
echo

payload=$(python3 -c "
import json, sys
print(json.dumps({'email': sys.argv[1], 'password': sys.argv[2]}))
" "$email" "$password")

http_status=$(curl -s -o /tmp/create_user_response.json -w "%{http_code}" \
    -X POST "$ENDPOINT" \
    -H "Content-Type: application/json" \
    -d "$payload")

case "$http_status" in
    201)
        echo "User created successfully."
        ;;
    403)
        echo "Error: request rejected — endpoint is only accessible from localhost." >&2
        exit 1
        ;;
    409)
        echo "Error: a user already exists. Bootstrap is only allowed when no users are present." >&2
        exit 1
        ;;
    *)
        echo "Error: unexpected HTTP status ${http_status}." >&2
        exit 1
        ;;
esac
