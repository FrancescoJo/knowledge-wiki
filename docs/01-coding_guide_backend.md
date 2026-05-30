# Backend Coding Guide

A supplement to `00-coding_guide.md` covering Kotlin/Spring backend–specific patterns.
Read `00-coding_guide.md` first; the rules there take precedence where they overlap.


## Kotlin Language Conventions

- `companion object` blocks must appear at the **bottom** of a class body. This matches the [official Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html#class-layout).


## Model Design

### Classification

Not all models are alike. Apply the right pattern for each category:

| Category | Characteristics | Mutable? |
|---|---|---|
| Entity | Has an identity (e.g., a database PK); goes through a defined lifecycle | Yes — use the Mutator pattern |
| Value Object | No identity; equality is defined by its properties alone | No — replace, never mutate |
| Configuration object | Assembled once at startup; structurally fixed thereafter | No |
| Read model / Projection | Query result; has no write path | No |

### Interface-First

Every **entity** and **value object** is exposed through a same-named interface. The concrete implementation is `internal` and hidden from callers.

Rationale: Kotlin interfaces support default implementations; hiding the concrete class preserves the freedom to change it without breaking call sites.

```kotlin
// Article.kt
interface Article {
    val id: UUID
    val isNew: Boolean
    val title: String
    val body: String
    val createdAt: Instant
    val updatedAt: Instant

    interface Mutator : Article {
        override var title: String
        override var body: String
    }

    companion object {
        fun create(title: String, body: String): Article {
            val now = Instant.now()
            return ArticleData(
                id = UuidCreator.getTimeOrderedEpoch(),
                isNew = true,
                title = title,
                body = body,
                createdAt = now,
                updatedAt = now,
            )
        }

        fun reconstitute(
            id: UUID,
            title: String,
            body: String,
            createdAt: Instant,
            updatedAt: Instant,
        ): Article = ArticleData(
            id = id,
            isNew = false,
            title = title,
            body = body,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}
```

Value objects follow the same interface-first rule. Since they are never mutated, they have no `Mutator` and the `internal data class` provides correct `equals`, `hashCode`, and `toString` automatically:

```kotlin
// SearchQuery.kt
interface SearchQuery {
    val keyword: String
    val limit: Int

    companion object {
        fun of(keyword: String, limit: Int): SearchQuery =
            SearchQueryData(keyword, limit)
    }
}

// SearchQueryData.kt
internal data class SearchQueryData(
    override val keyword: String,
    override val limit: Int,
) : SearchQuery
```

### Immutability by Default

The base interface exposes only `val` properties. Callers cannot modify a model unless they explicitly obtain a `Mutator` (see below).

### data class Implementations

Use `data class` for all immutable concrete implementations. This provides correct `equals`, `hashCode`, and `toString` without manual work.

```kotlin
// ArticleData.kt
internal data class ArticleData(
    override val id: UUID,
    override val isNew: Boolean,
    override val title: String,
    override val body: String,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : Article
```

The mutable counterpart (`ArticleMutator`) cannot be a `data class` when it must react to property changes. See the Exception section below.

#### Exception: Mutators with property-change side effects

When a `Mutator` implementation must react to property changes — for example,
advancing `updatedAt` on every field write — use a plain `class` with
`Delegates.observable` instead of a `data class`. A `data class` cannot back an
interface-overriding property with a delegate.

Apply this exception only to `Mutator` implementations. The immutable counterpart
(`XxxData`) remains a `data class`.

The implementing class must carry a comment explaining the deviation, and must
delegate `equals`, `hashCode`, and `toString` to a snapshot of the immutable
counterpart:

```kotlin
// ArticleMutator.kt
// Not a data class: Delegates.observable requires a regular class because
// data class does not support backing an interface-overriding property with
// a delegate. equals / hashCode / toString delegate to ArticleData snapshot.
internal class ArticleMutator(
    override val id: UUID,
    override val isNew: Boolean,
    title: String,
    body: String,
    override val createdAt: Instant,
    override var updatedAt: Instant,
) : Article.Mutator {

    override var title: String by Delegates.observable(title) { _, _, _ ->
        this.updatedAt = Instant.now()
    }

    override var body: String by Delegates.observable(body) { _, _, _ ->
        this.updatedAt = Instant.now()
    }

    private fun snapshot() = ArticleData(id, isNew, title, body, createdAt, updatedAt)

    override fun equals(other: Any?) = other is ArticleMutator && snapshot() == other.snapshot()
    override fun hashCode() = snapshot().hashCode()
    override fun toString() = snapshot().toString()
}
```

**Why snapshot delegation?**

This gives compile-time safety: adding a field to `ArticleMutator` breaks the
`snapshot()` call until `ArticleData` is updated too, and vice versa. The two
classes are kept in sync by the compiler.

### Sealed Interface for Closed Hierarchies

When a type has a known, closed set of subtypes, use `sealed interface`. This enables exhaustive `when` expressions and eliminates dead `else` branches.

```kotlin
sealed interface Shape
data class Circle(val radius: Double) : Shape
data class Rectangle(val width: Double, val height: Double) : Shape
```


## Mutator Pattern

Applies to **entities only**. Do not add a `Mutator` to value objects, configuration objects, or read models.

### Structure

The `Mutator` is a nested interface inside the entity interface. It extends the entity and overrides all mutable properties with `var`:

```kotlin
interface Article {
    val id: UUID
    val isNew: Boolean
    val title: String
    val body: String
    val createdAt: Instant
    val updatedAt: Instant

    interface Mutator : Article {
        override var title: String
        override var body: String
    }

    companion object {
        fun create(title: String, body: String): Article { ... }
        fun reconstitute(...): Article { ... }
    }
}
```

### Accessing a Mutator

`mutate()` is **not** a method on the base interface. It is exposed as an extension function co-located with the entity's implementation files. This keeps the mutation path out of the entity contract and limits visibility to layers that legitimately need it.

```kotlin
// ArticleExtensions.kt — same package as ArticleData / ArticleMutator
fun Article.mutate(): Article.Mutator = ArticleMutator(
    id = id,
    isNew = isNew,
    title = title,
    body = body,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
```

Callers receive the `Article` interface. They cannot obtain a `Mutator` by accident; they must explicitly import and call `mutate()`.

### Thread Safety

A `Mutator` instance must never be shared across threads or coroutines. Treat it as a short-lived, single-threaded scratchpad:

1. Obtain a `Mutator` from an entity.
2. Apply changes.
3. Persist or pass the result onward as the immutable entity type.

Passing a `Mutator` to another thread or suspending across a coroutine boundary is a defect.

### Layer Boundary Rules

`mutate()` calls outside the permitted layers are a code smell and warrant review:

| Layer | Permitted |
|---|---|
| Domain (entity behaviour, domain services) | Yes |
| Application (use cases) | Yes |
| Infrastructure (persistence mapping) | Conditionally — for ORM/DB mapping only, not business logic |
| Presentation (controllers, view models) | **No** |

Seeing `mutate()` in a controller or Thymeleaf helper is a sign that domain logic has leaked into the wrong layer.


## Domain Exception Design

Failures that occur inside domain logic must be communicated through the type system, not through special return values.

- **Do not use `null` or sentinel values to represent error conditions.** Returning `null` as a substitute for "not found" or "operation failed" forces callers to handle failures through control flow rather than types, and makes the failure reason invisible at the call site.
- **Define an explicit exception class for every distinct failure mode.** Each exception class is a first-class specification of the condition that caused it.
- **Reserve `null` returns for legitimate absent-value semantics.** A query method that may yield no result (`findById`, `findByEmail`) may return `null` to mean "nothing was found and that is normal." Command and mutation operations — those that change state — must always throw on failure; returning `null` from them is never appropriate.
- **Domain exceptions must not carry protocol concepts.** Domain logic has no knowledge of HTTP, gRPC, or any other transport. Responsibility for translating domain exceptions into protocol-level responses belongs to the outermost layer (e.g., a REST controller advice in the API module).

Project-specific exception hierarchy: see `01-coding_guide_omnimemo_backend.md`.


## Application Layer — Use Cases

Application services follow the **use case** pattern: one class per business scenario.

### Naming

`{Scenario}UseCase` — the scenario describes a specific business operation in active terms.

```kotlin
// Correct — named after the scenario
class CreateUserUseCase(...)
class LoginUseCase(...)
class UpdateUserEmailUseCase(...)

// Incorrect — named after the entity; grows unboundedly and obscures intent
class UserService(...)
```

### Method Naming

When a use case has a single method, name it with a descriptive verb phrase rather than a generic `execute()`. For use cases that group closely related query paths (e.g. `FindUserUseCase`), use the natural query verb for each variant.

```kotlin
class CreateUserUseCase(...) {
    fun create(email: String, rawPassword: String): User = ...
}

class FindUserUseCase(...) {
    fun findById(id: UserId): User? = ...
    fun findByEmail(email: String): User? = ...
}
```

### Service Naming Conventions Across Layers

| Layer | Suffix | Example | Notes |
|---|---|---|---|
| Application | `UseCase` | `CreateUserUseCase`, `LoginUseCase` | One class per business scenario |
| Domain | `Service` | `ArticleStatisticsService` | Cross-aggregate business logic; rare — name after the activity, not the aggregate |
| Infrastructure | `Service` | `JwtTokenService` | Technical concerns not covered by a core port |

Domain services are rare. Before writing one, verify the logic truly spans multiple aggregates and cannot live on any single entity.


## Outbound Ports

Outbound port interfaces (`Repository`, `PasswordHasher`, `TokenIssuer`, etc.) live in the **domain package** alongside the entities they serve. The infrastructure module depends on `backend-core`; never the reverse.

```
core.user.model/
  UserRepository.kt        ← port interface defined in the domain
  PasswordHasher.kt        ← port interface defined in the domain

infrastructure.user.persistence/
  UserRepositoryImpl.kt    ← implements UserRepository

infrastructure.security/   ← cross-cutting; no domain segment
  PasswordHasherImpl.kt    ← implements PasswordHasher (algorithm is domain-agnostic)
```

This placement makes the dependency inversion visible in the package structure without a separate `port/` package.


## Serialisation

The core domain model has no knowledge of serialisation formats (JSON, Protobuf, etc.). Annotations such as `@JsonProperty` or `@Serializable` must not appear on entity interfaces or their implementations.

Serialisation is the responsibility of the infrastructure layer. Infrastructure adapters translate between domain model types and wire formats using dedicated DTO classes or custom serialisers.

This boundary is enforced at the architecture level: `backend-core` must not depend on any serialisation library.


## Controller Conventions

### Controller Package Separation

View controllers (serving HTML/templates) and REST API controllers must reside in separate, clearly named packages. Mixing them in a single package creates ambiguity as the surface area grows.

| Package | Contents |
|---|---|
| `{root}.view` | `@Controller` classes that return views or `ModelAndView` |
| `{root}.api.endpoint.{domain}` | `@RestController` interfaces and their implementations |

### REST API Controller Design

Every REST API controller must be defined as an interface and implemented in a separate `impl` sub-package. This separation enables clean OpenAPI documentation annotations on the interface, keeps the implementation free of documentation noise, and makes the contract explicit.

```
{root}.api.endpoint.{domain}/Controller.kt          — interface (annotations, contract)
{root}.api.endpoint.{domain}/impl/ControllerImpl.kt — @RestController implementation
```

Rules for the interface:
- Declare all `@RequestMapping`, `@Operation`, `@ApiResponse`, and `@Tag` annotations here.
- Do not reference implementation details.

Rules for the implementation:
- Annotate the class with `@RestController` and mark it `internal`.
- Override every method from the interface; add no extra public surface.
- Keep all Spring-documentation annotations off the implementation class.

### DTO Package Layout

Request, response, and shared DTO types for a controller domain must be organised under a dedicated `dto` sub-package:

| Package | Contents |
|---|---|
| `{endpoint}.dto` | Shared/common DTO types |
| `{endpoint}.dto.request` | Request DTOs |
| `{endpoint}.dto.response` | Response DTOs |


## Testing Notes

### Full-Context Boundary

A test that starts the full Spring application context — for example via `@SpringBootTest(webEnvironment = RANDOM_PORT)` — is classified as **Large** even when every dependency (database, loopback server) is locally controlled. Full context startup exercises the entire wiring, security configuration, and migration stack; this fidelity belongs to the Large Test category. Apply the Q1/Q2 tree only to partial-context tests such as `@WebMvcTest` or `@DataJpaTest`.
