#!/usr/bin/env bash
# verify-notes.sh
#
# $Since: 2026-05-31T00:00:00Z
#
# Smoke-tests the Notes view layer against a running backend.
# Checks that the directory page and individual note pages respond with 200 HTML
# and contain expected content markers.
#
# Usage:
#   ./config/scripts/verify-notes.sh [BASE_URL]
#
# Arguments:
#   BASE_URL  Base URL of the running backend (default: http://localhost:8080)
#
# Requirements:
#   curl, python3

set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
RESPONSE_FILE=$(mktemp)
trap 'rm -f "$RESPONSE_FILE"' EXIT

pass=0
fail=0

check() {
    local label="$1"
    local url="$2"
    local expected_status="${3:-200}"
    shift 3
    local patterns=("$@")

    local status
    status=$(curl -s -o "$RESPONSE_FILE" -w "%{http_code}" -L "${url}") || {
        printf "  FAIL  %s — curl error (bad URL or connection)\n" "$label" >&2
        fail=$((fail + 1))
        return
    }

    if [[ "$status" != "$expected_status" ]]; then
        printf "  FAIL  %s — expected HTTP %s, got %s\n" "$label" "$expected_status" "$status" >&2
        fail=$((fail + 1))
        return
    fi

    local body
    body=$(cat "$RESPONSE_FILE")

    for pattern in "${patterns[@]}"; do
        if ! grep -qF "$pattern" <<< "$body"; then
            printf "  FAIL  %s — pattern not found: %s\n" "$label" "$pattern" >&2
            fail=$((fail + 1))
            return
        fi
    done

    printf "  ok    %s\n" "$label"
    pass=$((pass + 1))
}

# ── Connectivity ───────────────────────────────────────────────────────────────

probe_exit=0
curl -s --connect-timeout 5 --max-time 5 \
    -o /dev/null "${BASE_URL}/api/v1/health" 2>/dev/null || probe_exit=$?
if [[ $probe_exit -ne 0 ]]; then
    echo "Error: cannot reach ${BASE_URL} — is the server running?" >&2
    exit 1
fi

# ── Note count via API ─────────────────────────────────────────────────────────

en_count=$(curl -s "${BASE_URL}/api/v1/notes?language=en" \
    | python3 -c "
import json, sys
body = json.load(sys.stdin)['body']
print(sum(len(v) for v in body.values()))
")

ko_count=$(curl -s "${BASE_URL}/api/v1/notes?language=ko" \
    | python3 -c "
import json, sys
body = json.load(sys.stdin)['body']
print(sum(len(v) for v in body.values()))
")

echo "Notes in DB: EN=${en_count}  KO=${ko_count}"
if [[ "$en_count" -eq 0 && "$ko_count" -eq 0 ]]; then
    echo "Warning: no notes found — run seed-notes.sh first." >&2
fi

# ── View layer checks ──────────────────────────────────────────────────────────

echo ""
echo "View layer:"

check "/contents page" \
    "${BASE_URL}/contents" 200 \
    "href=\"/notes\""

check "/notes directory (EN)" \
    "${BASE_URL}/notes" 200 \
    "href=\"/\""

check "/notes directory includes breadcrumb links" \
    "${BASE_URL}/notes" 200 \
    "href=\"/contents\""

if [[ "$en_count" -gt 0 ]]; then
    mapfile -t title_info < <(curl -s "${BASE_URL}/api/v1/notes?language=en" \
        | python3 -c "
import json, sys, urllib.parse
body = json.load(sys.stdin)['body']
notes = [n for v in body.values() for n in v]
if notes:
    t = notes[0]['title']
    print(t)
    print(urllib.parse.quote(t, safe='/'))
")
    first_title="${title_info[0]:-}"
    first_title_encoded="${title_info[1]:-}"
    if [[ -n "$first_title" ]]; then
        check "note page: ${first_title}" \
            "${BASE_URL}/notes/${first_title_encoded}" 200 \
            "$first_title" "note-body" "note-editor-content"
    fi
fi

check "/notes/nonexistent returns 404" \
    "${BASE_URL}/notes/__nonexistent_note_title__" 404

# ── Summary ────────────────────────────────────────────────────────────────────

echo ""
echo "Results: ${pass} passed, ${fail} failed."
echo ""
echo "Manual checks:"
echo "  ${BASE_URL}/contents"
echo "  ${BASE_URL}/notes"
if [[ "$en_count" -gt 0 ]]; then
    echo "  ${BASE_URL}/notes/API%20Design%20Principles   (EN note)"
fi
if [[ "$ko_count" -gt 0 ]]; then
    echo "  ${BASE_URL}/notes/%EB%85%B8%ED%8A%B8%20%EA%B8%B0%EB%8A%A5%20%EC%84%A4%EA%B3%84%20%EC%9B%90%EC%B9%99   (KO note)"
fi

if [[ $fail -gt 0 ]]; then
    exit 1
fi
