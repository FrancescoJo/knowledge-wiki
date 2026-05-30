# OmniMemo Backend Coding Guide

OmniMemo project–specific rules for the Kotlin backend.
Read `00-coding_guide.md` and `01-coding_guide_backend.md` first; the rules there take precedence where they overlap.


## Module Structure

### Module Responsibilities

| Module | Responsibility |
|---|---|
| `backend-core` | Domain model, business logic, outbound port interfaces |
| `backend-infrastructure` | Port implementations — DB access, encryption, JWT issuance |
| `backend-api` | Spring Boot entry point, controllers, bean wiring |

### Packaging Principle: Domain First

Packages are organised **domain first, layer second**. The top-level segment after the module root identifies the business domain; the layer (`model`, `usecase`, `persistence`, …) is a sub-segment.

```
core.{domain}.model     — not core.model.{domain}
core.{domain}.usecase   — not core.usecase.{domain}
infrastructure.{domain}.persistence  — not infrastructure.persistence.{domain}
```

This makes the package structure "scream" what the system does — reading the top level shows the business domains, not the technical plumbing. Adding or removing a domain means touching one package subtree rather than multiple scattered packages.

**Exception — cross-cutting technical concerns.** Domain-first applies where the implementation is semantically specific to a business domain. Technical concerns that are inherently cross-cutting — cryptographic primitives, security algorithms, logging, caching — are placed in their own top-level package without a domain segment, even when they currently happen to serve only one domain. The reason: the classes themselves contain no business-domain concepts — they implement algorithms or infrastructure primitives that are equally applicable to any domain.

```
infrastructure.{concern}/             — cross-cutting utilities (domain-agnostic)
infrastructure.{domain}.persistence/  — persistence adapters (domain-specific)
```

Deciding which category a package falls into: ask whether the classes inside contain any business-domain concept. If removing all imports of `core.{domain}.*` would leave the class unchanged in meaning, it is cross-cutting.

### Package Layout

```
backend-core
  com.fj.omnimemo.core.{domain}.model/     ← domain model
    {Entity}.kt                — entity interface + companion factory methods
    {Entity}Id.kt              — typed identity (value object)
    {Entity}Data.kt            — immutable implementation (internal)
    {Entity}Mutator.kt         — mutable implementation (internal)
    {Entity}Extensions.kt      — extension functions (e.g. mutate())
    {Entity}Repository.kt      — outbound persistence port
    {Port}.kt                  — other outbound ports (e.g. PasswordHasher)

  com.fj.omnimemo.core.{domain}.usecase/   ← application layer
    {Scenario}UseCase.kt       — one class per business scenario

  com.fj.omnimemo.core.model/              ← cross-cutting base types (no domain)
    Persistable.kt
    DateTimeAuditable.kt

backend-infrastructure
  com.fj.omnimemo.infrastructure.{domain}.persistence/
    {Entity}RepositoryImpl.kt  — implements the port defined in backend-core

  com.fj.omnimemo.infrastructure.{concern}/  ← cross-cutting utilities; no domain segment
    {Impl}.kt

backend-api
  com.fj.omnimemo.api.endpoint.{domain}/
    {Resource}ApiController.kt
    {Resource}ViewController.kt
  com.fj.omnimemo.api.config/
    {Category}Configuration.kt — Spring bean wiring
```


## Domain Layer

### Sub-Package Responsibilities

Within a domain module, sub-packages are created only when there is content that belongs there — no package is required to exist in every domain.

Each sub-package has a fixed responsibility:

| Sub-package | Contents |
|---|---|
| `{domain}.model` | Domain interfaces and domain-language types (typed IDs, value objects). No concrete classes. |
| `{domain}.model.snapshot` | `internal` concrete implementations of model interfaces (immutable data classes, mutable scratchpad classes). Never referenced from outside the module. |
| `{domain}.repository` | Persistence port interfaces. One interface per aggregate root. |
| `{domain}.usecase` | Application-layer use cases. |
| `{domain}` (package level) | Extension functions and other utilities that operate on domain types but are not model definitions. |

Concern-specific sub-packages follow the same naming convention as the concern itself. For example, `{domain}.security` holds security-related port interfaces for that domain. These packages are created only when the domain actually has that kind of dependency — not as a template applied to every domain.

### OmniMemo Exception Hierarchy

All domain exceptions must follow the project exception hierarchy:

- `OmniMemoInternalException` — for invariant violations that the domain itself prohibits
- `OmniMemoExternalException` — for failures caused by external input or devices
- Both extend `OmniMemoException`, the sealed root

The sealed root enables exhaustive, uniform handling at the API boundary.


## ID Generation

Entity identities are assigned by the **domain layer** (inside the entity's `create`
factory method), not by the persistence layer.

- **Algorithm:** UUID v7 via `com.github.f4b6a3:uuid-creator`
- UUID v7 is time-ordered and collision-resistant. It is purely algorithmic and
  requires no network or database access, making it safe to generate in domain code.
- The `create()` companion factory generates the ID. Infrastructure receives
  entities with IDs already assigned and performs an unconditional `INSERT`.
- `reconstitute()` restores an entity from a persisted record with its stored ID.

**Rationale:**
- Entities are complete and testable before any persistence interaction.
- Domain events can carry the correct ID before the persistence commit.
- Repository implementations are simplified: `isNew == true` → INSERT, always.

This decision was made over the alternative of infrastructure-generated IDs.
The primary trade-off is that UUID v7 must be generated correctly within the domain;
however, since UUID v7 has negligible collision probability and needs no coordination,
this risk is accepted.


## API Layer

### API Versioning

Expose all REST API endpoints under a version prefix in the URL path. The current version prefix is `/v1`. Example: `/api/v1/users`.

When a new major version is introduced, add a new prefix (e.g., `/v2`) rather than modifying existing versioned paths.

### API Documentation

Use **Springdoc-OpenAPI** as the API documentation tool. Annotations belong on the controller interface, not on the implementation. Annotate every endpoint with `@Operation` and `@ApiResponse`; annotate every controller interface with `@Tag`.


## Static Analysis

Use **Detekt** for Kotlin static analysis. Configure it to run before the `compileKotlin` task so that violations are caught at build time.

- Address every Detekt finding. Prefer fixing the root cause over suppressing.
- When suppression is unavoidable, use `@Suppress("RuleId")` with a 1–2 line comment directly above it explaining the reason. A suppression without explanation is not acceptable.


## Utility Conventions

A utility is a stateless, context-free, pure function (or a group of closely related pure functions) that can be called from anywhere without external setup.

- Place utilities in a dedicated `util` package within the core module (e.g., `{core}.util`).
- Group related utilities into sub-packages by concern: `{core}.util.io`, `{core}.util.math`, etc. Utilities that do not fit a named concern live directly in `{core}.util`.
- Before writing a new utility, check whether an equivalent already exists. Reuse before creating.
- Before extending or modifying an existing utility, discuss the rationale. Utilities carry high dependency weight — a change affects every call site.
- Every utility must have exhaustive Small Tests that serve as its living specification.

**Utility & helper index** — consult this before writing any new helper code, and keep it up to date whenever a utility is added, changed, or removed:

- [`docs/02-coding_guide_utilities_and_helpers_backend.md`](02-coding_guide_utilities_and_helpers_backend.md)


## Infrastructure Layer Testing

Any layer that integrates with infrastructure concerns — including repositories, persistence adapters, and REST API controllers wired into a real application context — must be covered by both Small Tests and Medium Tests.

- **Small Tests** verify logic in isolation using test doubles.
- **Medium Tests** verify the integration with the actual infrastructure component (a real database, a real HTTP stack via `@WebMvcTest`, etc.).

For REST API modules, at least one Spock `@SpringBootTest` Large Test (smoke test) must be written per controller group. The smoke test must verify at least one happy-path scenario end-to-end with a fully loaded Spring context. Exhaustive case coverage — boundary conditions and error paths — belongs in Small and Medium Tests, not in the smoke test.

### Large Test Utility Conventions

Large Test helper code lives in `{root}.api.endpoint.test` and is never included in production source sets.

**API clients** — wrap all HTTP calls for a controller group in a single class. One client class per controller group:

| Class | Location |
|---|---|
| `HealthApiClient` | `…/test/health/HealthApiClient.kt` |
| `AuthApiClient` | `…/test/auth/AuthApiClient.kt` |
| `UserApiClient` | `…/test/user/UserApiClient.kt` |

Rules for API clients:
- Accept HTTP client and serialiser dependencies as constructor parameters; do not instantiate them internally.
- Reference URL paths through centralised path constants. Hardcoded URL strings are not permitted.
- Serialise request bodies through a shared mechanism. Inline string literals for structured payloads are not permitted.
- Manage protocol-level headers (authentication, CSRF, etc.) internally; callers must not construct headers manually.

**Shared helpers** — stateless utilities used internally by API clients or directly by specs for assertions:

- Helpers that manage protocol-level concerns (e.g. authentication headers, security tokens) must have restricted visibility so that only API clients use them. Specs must go through the API client, never construct protocol headers directly.
- Helpers that inspect responses for use in spec assertions (e.g. cookie extraction, status parsing) may be `public`. When specs are written in a different language from the helpers, apply the interoperability annotation required by the language boundary (e.g. `@JvmStatic` for Groovy calling a Kotlin `object`).

**Fixture** — encapsulates database reset and test data creation:

- A fixture class combines infrastructure cleanup (e.g. table truncation) with use-case-level setup (e.g. creating a test user) so that each spec's `setup` block can delegate its entire preparation to a single call.
- Specs must not contain direct database manipulation calls. All data preparation must go through a fixture.
