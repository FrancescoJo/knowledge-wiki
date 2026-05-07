# Knowledge Wiki — Tech Stack

## Backend

- **Language / Runtime:** Kotlin / JVM
- **Framework:** Spring Boot + Spring Web MVC
- SSE (Server-Sent Events) support: planned
- WebSocket support: planned

## Database

- **PostgreSQL + pgvector**
- Migration tool: Flyway or Liquibase
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
- Content serialization: JSON
- HTMX integration interface: to be designed at integration time

---

## Project Structure

With HTMX, HTML templates are rendered server-side and live inside the backend module.
`frontend/` therefore contains only reusable client-side libraries, not per-application modules.

```
backend/
  wiki/
    app/
      src/main/resources/
        templates/      # Thymeleaf (or equivalent) templates
        static/         # compiled frontend assets land here
    core/
    infrastructure/
  wiki-admin/           # empty — implementation deferred to v1.0
  common-libs/
    lib-1/
    lib-2/
    ...

frontend/
  common-libs/
    textedit/           # standalone rich text editor module
```

---

## Development Workflow

Backend and frontend toolchains run in parallel during development:

| Process | Role |
|---|---|
| Spring Boot (devtools) | Serves templates and static assets; auto-reloads on class/resource changes |
| Vite (watch mode) | Compiles `textedit` TypeScript; outputs to `backend/wiki/app/src/main/resources/static/` |

> Spring Boot must always be running during development — there is no standalone frontend dev server.
> This is a characteristic of HTMX (server-rendered HTML), not a limitation.

---

## Build Scripts

`build-wiki` and `build-wiki-admin` are the top-level build entry points (Gradle-based).

### Supported Goals

| Goal | Description |
|---|---|
| `lint` | Alias for `lint-all` |
| `lint-backend` | Lint backend only |
| `lint-frontend` | Lint frontend only |
| `lint-all` | Lint everything |
| `test` | Alias for `test-all` |
| `test-backend-small` | Backend small tests |
| `test-backend-medium` | Backend medium tests |
| `test-backend-large` | Backend large tests |
| `test-backend-all` | All backend tests |
| `test-frontend-small` | Frontend small tests |
| `test-frontend-medium` | Frontend medium tests |
| `test-frontend-large` | Frontend large tests |
| `test-frontend-all` | All frontend tests |
| `test-all` | All tests |
| `run` | Alias for `run-all` |
| `run-backend` | Start backend (Spring Boot) |
| `run-frontend` | Start frontend toolchain (Vite watch) |
| `run-all` | Start both |
| `package` | Build textedit → copy to `backend/wiki/app/src/main/resources/static/` → assemble single JAR |
