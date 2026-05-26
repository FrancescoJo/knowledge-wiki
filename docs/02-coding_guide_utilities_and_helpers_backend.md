# Backend Utilities & Helpers Index

Index of all shared utility and helper code in the backend modules.
Keep this file up to date whenever a helper is added, changed, or removed.

Entries follow the format:
```
(Call path)  :  (one-line description)  [source file]
```

---

## Production Utilities — `backend-core`

> Package: `com.fj.omnimemo.core.util`
> Source: `backend-core/src/main/kotlin/com/fj/omnimemo/core/util/`

| Call path | Description | File |
|---|---|---|
| `parseUuidOrNull(raw: String): UUID?` | Parses a string as a UUID; returns `null` if malformed | `util/Parsing.kt` |

---

## Test Fixtures — `backend-core`

> Package: `com.fj.omnimemo.core.*`
> Source: `backend-core/src/testFixtures/kotlin/`

### Test Size Annotations

| Annotation | Description | File |
|---|---|---|
| `@SmallTest` | Marks a unit test with no external dependencies | `test/annotation/SmallTest.kt` |
| `@MediumTest` | Marks an integration test with locally controlled dependencies | `test/annotation/MediumTest.kt` |
| `@LargeTest` | Marks a full-context or E2E test with real external dependencies | `test/annotation/LargeTest.kt` |

### Mock Implementations (in-memory test doubles)

| Class | Public methods | Description | File |
|---|---|---|---|
| `MockTokenIssuer` | `issue(subject): String` | Returns `"token:<subject>"` without signing | `security/MockTokenIssuer.kt` |
| `MockPasswordHasher` | `hash(raw): String`, `matches(raw, hash): Boolean` | Prefixes hashes with `"hashed:"` | `user/security/MockPasswordHasher.kt` |
| `MockUserRepository` | `findById`, `findByEmail`, `save`, `delete`, `clear` | In-memory `UserRepository` backed by `MutableMap` | `user/repository/MockUserRepository.kt` |
| `MockRefreshTokenRepository` | `save`, `findByToken`, `delete`, `clear` | In-memory `RefreshTokenRepository` backed by `MutableMap` | `user/repository/MockRefreshTokenRepository.kt` |

---

## Test Fixtures — `backend-infrastructure`

> Package: `com.fj.omnimemo.infrastructure.*`
> Source: `backend-infrastructure/src/testFixtures/kotlin/`

| Class | Public methods | Description | File |
|---|---|---|---|
| `PostgresContainerSupport` | `newContainer(): PostgreSQLContainer<*>` | Creates a fresh `postgres:16-alpine` Testcontainers instance | `test/PostgresContainerSupport.kt` |
| `UserTableFixture(jdbc)` | `deleteAll()`, `findEmailEncryptedBytes(id): ByteArray` | Raw SQL helpers for the `users` table; column names sourced from `UserRepositoryImpl` | `user/persistence/UserTableFixture.kt` |

---

## Test Helpers — `backend-api`

> Package: `com.fj.omnimemo.api.endpoint.test`
> Source: `backend-api/src/test/kotlin/com/fj/omnimemo/api/endpoint/test/`

### Fixture

| Class | Public methods | Description | File |
|---|---|---|---|
| `ApiFixture(createUserUseCase, jdbc)` | `resetWithUser(email, password): User` | Resets DB via `UserTableFixture` and creates a test user in one call | `ApiFixture.kt` |

### Protocol Helpers (internal — API clients only)

| Object | Public methods | Description | File |
|---|---|---|---|
| `CsrfSupport` *(internal)* | `buildHeaders(withJson, cookies): HttpHeaders` | Builds double-submit cookie CSRF header pairs using a random UUID | `CsrfSupport.kt` |

### Response Helpers

| Object | Public methods | Description | File |
|---|---|---|---|
| `CookieSupport` | `cookieValue(response, name): String?`, `cookieMaxAge(response, name): Int` | Extracts cookie value and `Max-Age` from `ResponseEntity`; `@JvmStatic` for Groovy interop | `CookieSupport.kt` |

### API Clients (Large Test HTTP wrappers)

| Class | Public methods | Description | File |
|---|---|---|---|
| `HealthApiClient(restTemplate)` | `health(): ResponseEntity<String>` | Wraps `GET /api/v1/health` | `health/HealthApiClient.kt` |
| `AuthApiClient(restTemplate, objectMapper)` | `login(email, password)`, `logout(accessToken, refreshToken)`, `refresh(refreshToken)`, `loginAndGetAccessToken(email, password): String?` | Wraps auth endpoints; manages CSRF headers internally | `auth/AuthApiClient.kt` |
| `UserApiClient(restTemplate, objectMapper)` | `findById(id, accessToken)`, `create(email, password, accessToken)`, `delete(id, accessToken)` | Wraps user endpoints; manages CSRF headers internally | `user/UserApiClient.kt` |
