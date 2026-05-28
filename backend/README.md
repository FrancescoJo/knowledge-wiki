# backend

Spring Boot backend for Knowledge Wiki.

- Kotlin / JVM, Spring Boot 3.3.0
- Spring Web MVC + Thymeleaf for server-rendered pages
- Spring Security with JWT (httpOnly cookie)
- Spring JDBC + PostgreSQL; schema managed by Liquibase
- Email encrypted at rest with AES-256-GCM; lookups use an HMAC-SHA256 blind index

---

## Modules

| Module | Role |
|---|---|
| `backend-core` | Domain model and repository interfaces. Pure Kotlin — no Spring dependency. |
| `backend-infrastructure` | Spring JDBC implementations, AES-GCM / HMAC utilities, Liquibase changelogs. |
| `backend-api` | Spring Boot entry point, HTTP controllers, Thymeleaf templates. |

---

## File Structure

```
backend-core/
  src/main/kotlin/               — Domain entities, value objects, repository interfaces
  src/test/kotlin/               — Small tests
  src/testFixtures/kotlin/       — @SmallTest / @MediumTest / @LargeTest annotations
                                   MockUserRepository and other test doubles

backend-infrastructure/
  src/main/kotlin/
    persistence/user/            — UserRepositoryImpl (Spring JDBC)
    security/                    — AesGcmCipher, HmacBlindIndex, CryptoConfiguration
  src/main/resources/
    db/changelog/                — Liquibase changelogs (SQL format)
  src/test/kotlin/               — Medium tests (Testcontainers + PostgreSQL)
  src/testFixtures/kotlin/       — PostgresContainerSupport

backend-api/
  src/main/kotlin/               — OmnimemoApplication, controllers, advice
  src/main/resources/
    templates/                   — Thymeleaf HTML templates
    application.yml              — Public configuration (committed)
  application-secret.yml         — Secret overrides (gitignored — see Local Setup)
  application-secret.yml.template
  src/test/groovy/               — Spock specifications
```

---

## Local Setup

The application requires a secrets file that is not committed to version control.

```bash
cp backend-api/application-secret.yml.template backend-api/application-secret.yml
```

Then edit `application-secret.yml` and fill in the values.
Generate each key with:

```bash
openssl rand -base64 32
```

| Property | Description |
|---|---|
| `spring.datasource.password` | PostgreSQL password |
| `app.security.aes-key` | AES-256 key for email encryption (base64) |
| `app.security.hmac-key` | HMAC-SHA256 key for blind index (base64) |
| `app.security.jwt-signing-key` | JWT signing key (base64) |

`application.yml` sets the database URL and username; override them in the secrets file if needed.

---

## Infrastructure

A Docker Compose file in `config/infrastructure/` starts the required services (PostgreSQL with pgvector).
The PostgreSQL password is not hardcoded — it is read from `config/infrastructure/.env` (gitignored).

**First-time setup:**

```bash
cp config/infrastructure/.env.template config/infrastructure/.env
# Edit .env and set OMNIMEMO_DB_PASSWORD to match spring.datasource.password in application-secret.yml
```

**Start:**

```bash
bash config/infrastructure/up.sh
```

| Property | Value |
|---|---|
| Host | `localhost:5432` |
| Database | `omnimemo` |
| Username | `omnimemo` |
| Password | value of `OMNIMEMO_DB_PASSWORD` in `.env` |

To stop and remove containers (data is preserved in a named volume):

```bash
docker compose -f config/infrastructure/docker-compose.yml down
```

To also remove the data volume:

```bash
docker compose -f config/infrastructure/docker-compose.yml down -v
```

---

## Development

Prerequisites: complete [Local Setup](#local-setup) and start the [Infrastructure](#infrastructure) first.

```bash
cd backend
./gradlew :backend-api:bootRun
```

The app starts on `http://localhost:8080`.

| URL | Description |
|---|---|
| `http://localhost:8080` | Application root |
| `http://localhost:8080/swagger-ui/index.html` | Swagger UI (API explorer) |
| `http://localhost:8080/v3/api-docs` | OpenAPI specification (JSON) |

To build a runnable JAR:

```bash
./gradlew :backend-api:bootJar
java -jar backend-api/build/libs/omnimemo-*.jar
```

---

## Testing

```bash
./gradlew test-backend-small    # Small tests — no external dependencies
./gradlew test-backend-medium   # Medium tests — Testcontainers (requires Docker)
./gradlew test-backend-large    # Large tests
./gradlew test-backend-all      # All backend tests
```

Or per-module:

```bash
./gradlew :backend-infrastructure:testSmall
./gradlew :backend-infrastructure:testMedium
```

### Test sizes

| Size | Description |
|---|---|
| Small | Pure unit tests. No network, no filesystem, no database. Mocks for all external dependencies. |
| Medium | Integration tests. Testcontainers spins up a real PostgreSQL instance per test class. Docker required. |
| Large | End-to-end tests against production-like infrastructure. |

Test-size annotations (`@SmallTest`, `@MediumTest`, `@LargeTest`) are defined in `backend-core`'s
`testFixtures` source set and are available to all modules via:

```kotlin
testImplementation(testFixtures(project(":backend-core")))
```

### Test framework by module

| Module | Framework |
|---|---|
| `backend-core` | JUnit 5 + kotlin-test |
| `backend-infrastructure` | JUnit 5 + kotlin-test + Testcontainers |
| `backend-api` | Spock (Groovy 4 / JUnit Platform) |
