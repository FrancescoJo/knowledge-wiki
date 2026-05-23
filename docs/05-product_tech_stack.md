# Knowledge Wiki — Tech Stack

## Backend

- **Language / Runtime:** Kotlin / JVM
- **Framework:** Spring Boot + Spring Web MVC
- SSE (Server-Sent Events) support: planned
- WebSocket support: planned

## Database

- **PostgreSQL**
- **Migration tool:** Liquibase (Community Edition, Apache 2.0)
  - Rationale: free and open-source including rollback support; Flyway Community does not include rollback without a paid licence
- **Access layer:** Spring JDBC Template — SQL written explicitly; no ORM
  - Rationale: straightforward and predictable; infrastructure replacement is rare, so writing SQL manually is an acceptable trade-off
- `pgvector` extension will be used for vector search (introduced in v0.3)

## Frontend

- **HTMX** — server-driven UI, partial page updates, document navigation
- **TypeScript** — client-side interactivity; primary language of the `textedit` module
- **No UI framework** — React, Vue, and similar frameworks are explicitly excluded

### textedit Module

- Location: `frontend/common-libs/textedit/`
- Language: TypeScript only
- Editor library: **TipTap core** (`@tiptap/core`) — no React bindings
- Responsibility: editor role only (no routing, no API calls)
- Content serialisation: JSON
- HTMX integration interface: to be designed at integration time

---

## Entity Identity

- **ID type:** UUID v7
- **Library:** `com.github.f4b6a3:uuid-creator` (MIT licence) — declared in `backend-core`; available as a transitive dependency to other modules
- **Generation site:** domain layer (`create` factory on each entity), not the persistence layer
- **Rationale:** UUID v7 is time-ordered, collision-resistant, and purely algorithmic. Generating IDs in the domain keeps entities complete and testable before any DB interaction, enables domain events with correct IDs before commit, and simplifies repositories (new entity always means INSERT). See `docs/01-coding_guide_backend.md § ID Generation` for full reasoning.

---

## Authentication

### Login Token

- **Strategy:** Stateless JWT stored in **httpOnly cookie**
- **Stay-logged-in:** `Max-Age` on the cookie combined with JWT expiry time
- **Refresh token:** httpOnly cookie as well, if introduced
- **Rationale:** Stateless token avoids server-side session management overhead. httpOnly cookie prevents XSS access to the token. HTMX compatibility is preserved because the browser sends cookies automatically on every request.

### Password Storage

- **Algorithm:** bcrypt via Spring Security Crypto
- **Rationale:** Industry-standard adaptive hashing; resistant to brute-force as cost factor can be raised over time.

### Sensitive Data Encryption

- **Scope:** Password (bcrypt hash), email and other PII (AES-GCM encryption)
- **Email encryption scheme:** AES-GCM with a per-record random IV; IV stored alongside the ciphertext
- **Email searchability:** Blind Index pattern — an HMAC-SHA256 digest of the plaintext email is stored in a separate column (`email_hmac`) and used for `WHERE` lookups; the encrypted column is used only for display/decryption
- **Encryption boundary:** Encryption and decryption are handled entirely within the infrastructure layer (Repository/DAO); the core and API layers operate on plaintext values only
- **Rationale:** Encrypting only at the infrastructure boundary keeps business logic free of cryptographic concerns. The Blind Index pattern enables efficient indexed lookups without deterministic encryption (which would leak block patterns).

### Key Management

- **AES-GCM key, HMAC key, JWT signing key** are all loaded from `application.yml`
- The `application.yml` containing secrets is **gitignored** and managed per environment outside of source control
- **Rationale:** Sufficient for personal-use deployment. Vault or KMS would be appropriate if the project grows to a multi-operator setup.

### Authorisation System

- **Library:** Spring Security (full adoption)
- **JWT library:** nimbus-jose-jwt (bundled with Spring Security — no extra dependency)
- **JWT verification:** custom `OncePerRequestFilter` that validates the JWT from the httpOnly cookie on every request
- **Access model (v0.1):** anonymous users — read only; authenticated users — read and write
- **Rationale:** Spring Security provides a well-tested filter chain and integrates cleanly with the JWT-based stateless model.

### CSRF Protection

- Applied; HTMX-compatible configuration (custom request header or SameSite cookie attribute)
- **Rationale:** The JWT is carried in a cookie, so the browser sends it automatically — CSRF protection is necessary regardless of the token strategy.

### HTTPS

- Terminated at the **nginx reverse proxy**; not handled at the application level
- Local development uses plain HTTP for convenience
- **Rationale:** The application never sees unencrypted traffic from external clients. Adding TLS at the application layer would be redundant.

---

## Project Structure

With HTMX, HTML templates are rendered server-side and live inside the backend module.
`frontend/` therefore contains only reusable client-side libraries, not per-application modules.

```
backend/                         ← Gradle root project
  backend-api/                   ← Spring Boot application
    src/main/kotlin/             ← controllers, application entry point
    src/main/resources/
      templates/                 ← Thymeleaf templates
      static/                    ← compiled frontend assets land here
  backend-core/                  ← domain models, business logic, repository interfaces
  backend-infrastructure/        ← DB access, external service adapters (added in v0.1)
  wiki-admin/                    ← empty skeleton; implementation deferred to v1.0

frontend/
  common-libs/
    textedit/                    ← standalone rich text editor module
```

---

## Development Workflow

Backend and frontend toolchains run in parallel during development:

| Process | Role |
|---|---|
| Spring Boot (devtools) | Serves templates and static assets; auto-reloads on class/resource changes |
| Vite (watch mode) | Compiles `textedit` TypeScript; outputs to `frontend/common-libs/textedit/dist-bundle/` |

Gradle's `processResources` task copies the textedit bundle from `dist-bundle/` into `backend-api/src/main/resources/static/lib/` at build time.

> Spring Boot must always be running during development — there is no standalone frontend dev server.
> This is a characteristic of HTMX (server-rendered HTML), not a limitation.
