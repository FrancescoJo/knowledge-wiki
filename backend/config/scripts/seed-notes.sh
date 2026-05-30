#!/usr/bin/env bash
# seed-notes.sh
#
# $Since: 2026-05-31T00:00:00Z
#
# Creates dummy note data for local development via the REST API.
# Requires a running backend and at least one registered user account.
#
# Usage:
#   ./config/scripts/seed-notes.sh [BASE_URL]
#
# Arguments:
#   BASE_URL  Base URL of the running backend (default: http://localhost:8080)
#
# Requirements:
#   curl, python3

set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
COOKIE_JAR=$(mktemp)
RESPONSE_FILE=$(mktemp)
trap 'rm -f "$COOKIE_JAR" "$RESPONSE_FILE"' EXIT

# ── Connectivity check ─────────────────────────────────────────────────────────

probe_exit=0
curl -s --connect-timeout 5 --max-time 5 \
    -o /dev/null "${BASE_URL}/api/v1/health" 2>/dev/null || probe_exit=$?
if [[ $probe_exit -ne 0 ]]; then
    echo "Error: cannot reach ${BASE_URL} — is the server running?" >&2
    exit 1
fi

# ── Obtain initial CSRF token ──────────────────────────────────────────────────

curl -s -c "$COOKIE_JAR" -b "$COOKIE_JAR" -o /dev/null "${BASE_URL}/"

csrf_raw=$(awk -F'\t' '/XSRF-TOKEN/{print $NF}' "$COOKIE_JAR")
if [[ -z "$csrf_raw" ]]; then
    echo "Error: could not obtain CSRF token." >&2
    exit 1
fi
csrf_token=$(python3 -c \
    "import sys, urllib.parse; print(urllib.parse.unquote(sys.stdin.read().strip()))" \
    <<< "$csrf_raw")

# ── Authenticate ───────────────────────────────────────────────────────────────

read -r -p "Email: " email
read -r -s -p "Password: " password
echo

http_status=$(curl -s \
    -c "$COOKIE_JAR" -b "$COOKIE_JAR" \
    -o "$RESPONSE_FILE" -w "%{http_code}" \
    -X POST "${BASE_URL}/api/v1/auth/login" \
    -H "X-XSRF-TOKEN: ${csrf_token}" \
    --data-urlencode "email=${email}" \
    --data-urlencode "password=${password}")

case "$http_status" in
    200) echo "Authenticated." ;;
    401) echo "Error: invalid email or password." >&2; exit 1 ;;
    *)   echo "Error: login failed (HTTP ${http_status})." >&2; exit 1 ;;
esac

# ── Create note helper ─────────────────────────────────────────────────────────

created=0
skipped=0
failed=0

create_note() {
    local language="$1"
    local title="$2"
    local content="$3"

    local payload
    payload=$(python3 -c "
import json, sys
data = {
    'language': sys.argv[1],
    'title':    sys.argv[2],
    'content':  sys.argv[3],
    'accessLevel': 'PUBLIC',
    'status':      'PUBLISHED',
    'summary':     None,
}
print(json.dumps(data))
" "$language" "$title" "$content")

    local status
    status=$(curl -s \
        -c "$COOKIE_JAR" -b "$COOKIE_JAR" \
        -o "$RESPONSE_FILE" -w "%{http_code}" \
        -X POST "${BASE_URL}/api/v1/notes" \
        -H "Content-Type: application/json" \
        -H "X-XSRF-TOKEN: ${csrf_token}" \
        -d "$payload")

    case "$status" in
        201) printf "  + [%s] %s\n" "$language" "$title"; created=$((created + 1)) ;;
        409) printf "  ~ [%s] %s (already exists)\n" "$language" "$title"; skipped=$((skipped + 1)) ;;
        *)   printf "  ! [%s] %s (HTTP %s)\n" "$language" "$title" "$status" >&2
             failed=$((failed + 1)) ;;
    esac
}

# ── English notes ──────────────────────────────────────────────────────────────

echo ""
echo "English notes:"

create_note "en" "API Design Principles" \
'# API Design Principles

Good API design follows a consistent set of principles that make the interface
predictable and easy to consume.

## RESTful Conventions

- Use nouns for resource names, not verbs
- Prefer plural resource names (`/notes`, not `/note`)
- Use HTTP methods semantically: GET reads, POST creates, PUT replaces, DELETE removes

## Versioning

Always version your API from day one. Use the URI path (`/api/v1/`) so that
breaking changes can be introduced without removing the existing surface.

## Error Responses

Return structured error bodies with a machine-readable code and a human-readable
message. Include a request ID for traceability.

```json
{
  "error": "NOTE_NOT_FOUND",
  "message": "Note with the given ID does not exist.",
  "requestId": "abc-123"
}
```'

create_note "en" "Backend Architecture" \
'# Backend Architecture

## Layered Architecture

The backend follows a strict layered architecture:

| Layer | Responsibility |
|---|---|
| API | HTTP handling, request/response DTO conversion |
| Core | Domain model, use cases, repository interfaces |
| Infrastructure | Database, external services, repository implementations |

Dependencies point inward only. The core layer has no dependency on Spring or JDBC.

## Module Structure

```
backend-api/            → Spring Boot app, controllers, security
backend-core/           → Domain model, use cases (pure Kotlin)
backend-infrastructure/ → JDBC implementations, Liquibase migrations
```'

create_note "en" "Clean Code Guidelines" \
'# Clean Code Guidelines

## Naming

Names should reveal intent. If a name requires a comment to explain it, rename it.

```kotlin
// Bad
val d: Int  // elapsed time in days

// Good
val elapsedTimeInDays: Int
```

## Functions

Functions should do one thing. If a function does more than one thing, extract it.

Keep functions small — the ideal function fits in one screen and has a single level
of abstraction throughout.

## Comments

Comments are a failure to express intent in code. Prefer self-documenting code.
Only comment when the *why* is non-obvious: a hidden constraint, a subtle invariant,
or a workaround for a specific bug.'

create_note "en" "Database Conventions" \
'# Database Conventions

## Naming

- Tables: `snake_case`, plural (`notes`, `note_versions`)
- Columns: `snake_case` (`created_at`, `author_id`)
- Constraints: `pk_table`, `fk_table_column`, `uq_table_column`, `idx_table_column`

## Primary Keys

Use UUID v7 for all primary keys. UUID v7 is time-ordered, which provides
good index locality for B-tree indexes while remaining globally unique.

## Timestamps

All timestamp columns use `TIMESTAMPTZ` (timestamp with time zone).
Store and retrieve in UTC. Never store local times in the database.'

create_note "en" "Git Workflow" \
'# Git Workflow

## Branch Strategy

- `main` — production-ready code only
- `v0.x` — version feature branches; all feature work is committed here directly

## Commit Messages

Follow the Conventional Commits format:

```
type(scope): short summary in imperative mood

Longer explanation if needed. Focus on *why*, not *what*.
```

Types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`

## Commit Discipline

Each commit must have a single logical reason and must build and pass all tests.
Squash fixup commits before merging.'

# ── Korean notes ───────────────────────────────────────────────────────────────

echo ""
echo "Korean notes:"

create_note "ko" "가이드라인 문서 작성법" \
'# 가이드라인 문서 작성법

## 목적

좋은 가이드라인 문서는 새로운 팀원이 자립적으로 작업할 수 있도록 돕습니다.

## 구성 원칙

- **두괄식 작성**: 핵심 내용을 먼저, 세부 내용은 나중에
- **예시 포함**: 추상적인 설명보다 구체적인 예시가 이해를 돕습니다
- **최신성 유지**: 문서는 코드와 동기화되어야 합니다

## 체크리스트

- [ ] 목적과 대상 독자를 명시했는가?
- [ ] 선행 지식이 필요한 경우 참조 링크를 제공했는가?
- [ ] 예시 코드가 실제로 동작하는가?'

create_note "ko" "노트 기능 설계 원칙" \
'# 노트 기능 설계 원칙

## 핵심 가치

- **무결성**: 버전 관리와 Optimistic Locking으로 데이터 손실 방지
- **접근 제어**: PUBLIC / RESTRICTED / PRIVATE 세 단계
- **충돌 안전성**: 편집 충돌 감지 및 localStorage 임시 저장

## 컨텐츠 저장 형식

모든 노트는 **Markdown**으로 저장됩니다. TipTap JSON은 편집 중 클라이언트
메모리에만 존재하며, 저장 시 Markdown으로 직렬화됩니다.

## 버전 저장 전략

| 버전 | 저장 방식 |
|---|---|
| 1, 10의 배수 | 전체 스냅샷 (`is_snapshot = true`) |
| 그 외 | unified diff delta |'

create_note "ko" "데이터베이스 설계 노트" \
'# 데이터베이스 설계 노트

## ERD 개요

```
users
  └─ notes (author_id)
       ├─ note_versions   (컨텐츠 저장)
       ├─ note_audits     (행위 이력)
       ├─ note_tags       (N:M)
       └─ note_contributors  (Private 접근 추적)
```

## 인덱스 전략

자주 실행되는 쿼리 패턴 기준으로 인덱스를 생성합니다.

- `(language, title_index)` — 언어별 디렉토리 목록 조회
- `note_id` on `note_audits` — 노트별 이력 조회'

create_note "ko" "리뷰 프로세스" \
'# 리뷰 프로세스

## 목적

코드 리뷰는 버그를 찾는 것이 아니라 **지식을 공유**하고 **설계를 개선**하는 과정입니다.

## 리뷰어 체크리스트

- 변경의 목적이 명확한가?
- 경계 조건(빈 입력, null, 최댓값)이 처리되어 있는가?
- 테스트가 충분한가?
- 코딩 가이드라인을 준수하는가?

## 피드백 원칙

- 코드에 대해 논평하고, 사람에 대해 논평하지 않는다
- 제안은 이유와 함께 제시한다
- 칭찬도 명시적으로 남긴다'

create_note "ko" "배포 절차" \
'# 배포 절차

## 전제 조건

배포 전 다음 사항을 확인합니다.

1. 모든 테스트 통과
2. 코드 리뷰 승인
3. 스테이징 환경 검증 완료

## 배포 단계

```bash
# 1. 빌드
./gradlew build

# 2. 데이터베이스 마이그레이션 확인
./gradlew :backend-infrastructure:liquibaseStatus

# 3. 서버 배포 (무중단)
java -jar backend-api/build/libs/omnimemo.jar
```

## 롤백

배포 실패 시 이전 JAR 파일로 즉시 롤백하고, DB 롤백이 필요하면
Liquibase rollback을 사용합니다.'

# ── Summary ────────────────────────────────────────────────────────────────────

echo ""
echo "Done: ${created} created, ${skipped} skipped, ${failed} failed."
if [[ $failed -gt 0 ]]; then
    exit 1
fi
