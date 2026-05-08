# Knowledge Wiki — Project Rules

## Coding Guide

`docs/00-coding_guide.md` is the authoritative coding standard for this project.
**Read it before starting any implementation task. All code produced must comply with it.**

Key rules from that guide that are easy to overlook:

- Do not rush to code. State assumptions explicitly and surface ambiguities before writing anything.
- Write only the minimum code needed. No speculative abstractions, no unrequested features.
- Keep changes minimal. Do not "improve" adjacent code, comments, or formatting unless directly asked.
- A commit must have a single logical reason and must build and pass all tests.
- Do not add any AI attribution in codebase.

## Language

All code elements, comments, identifiers, and documentation use **British English**.

| British | Not |
|---|---|
| serialise | serialize |
| initialise | initialize |
| colour | color |
| behaviour | behavior |
| licence (noun) | license (noun) |
| analyse | analyze |


## Explicit Confirmation Required

Stop and explain your reasoning before proceeding with any of the following:

- Modifying or deleting existing code on the grounds that it "looks unused" or because tests are now failing
- Any decision that affects architecture, module boundaries, or technology choices


## Tech Stack

Full details: `docs/05-product_tech_stack.md`

| Layer | Choice |
|---|---|
| Backend | Kotlin / JVM, Spring Boot, Spring Web MVC |
| Database | PostgreSQL + pgvector, Flyway or Liquibase |
| Frontend | HTMX + TypeScript — **no React, no Vue, no UI framework** |
| Editor | TipTap core (`@tiptap/core`) — no React bindings |


## Project Structure

Full details: `docs/05-product_roadmap.md`, `docs/05-product_tech_stack.md`

```
docs/                          — project documentation
frontend/
  common-libs/
    textedit/                  — standalone rich text editor (TipTap core wrapper)
backend/                       — planned
  wiki/
  wiki-admin/                  — empty skeleton only; implementation deferred to v1.0
```


## Frontend Module Conventions

Every frontend module follows this layout:

```
src/      — implementation source only
test/     — test code only; excluded from the build output
dev/      — development-only files (index.html, bootstrap scripts)
dist/     — build output (gitignored)
```

### TypeScript path alias

`@src/` resolves to `src/` within each module.
Always configure this in **both** `tsconfig.json` (`paths`) and `vitest.config.ts` (`resolve.alias`).

```typescript
// tsconfig.json
"baseUrl": ".",
"paths": { "@src/*": ["src/*"] }

// vitest.config.ts
resolve: { alias: { '@src': resolve(__dirname, 'src') } }
```

### Test size

All tests in `frontend/` are **Small** tests (no network, no filesystem, no database).
Use jsdom via Vitest. Test files live in `test/`, not `src/`.
