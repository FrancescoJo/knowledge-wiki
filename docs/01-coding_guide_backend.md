# Backend Coding Guide

A supplement to `00-coding_guide.md` covering Kotlin backend–specific patterns.
Read `00-coding_guide.md` first; the rules there take precedence where they overlap.


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
                id = UUID.randomUUID(),
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


## Serialisation

The core domain model has no knowledge of serialisation formats (JSON, Protobuf, etc.). Annotations such as `@JsonProperty` or `@Serializable` must not appear on entity interfaces or their implementations.

Serialisation is the responsibility of the infrastructure layer. Infrastructure adapters translate between domain model types and wire formats using dedicated DTO classes or custom serialisers.

This boundary is enforced at the architecture level: `backend-core` must not depend on any serialisation library.
