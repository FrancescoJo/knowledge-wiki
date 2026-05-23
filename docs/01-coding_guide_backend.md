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

Every entity is exposed through a same-named interface. The concrete implementation is `internal` and hidden from callers.

Rationale: Kotlin interfaces support default implementations; hiding the concrete class preserves the freedom to change it without breaking call sites.

```kotlin
interface Article {
    val title: String
    val body: String

    companion object {
        fun create(title: String, body: String): Article =
            ArticleData(title = title, body = body)
    }
}
```

### Immutability by Default

The base interface exposes only `val` properties. Callers cannot modify a model unless they explicitly obtain a `Mutator` (see below).

### data class Implementations

Use `data class` for all concrete implementations. This provides correct `equals`, `hashCode`, and `toString` without manual work.

Two distinct implementations per entity:

| Class | Properties | Implements |
|---|---|---|
| `ArticleData` | `val` | `Article` |
| `ArticleMutator` | `var` | `Article.Mutator` |

```kotlin
internal data class ArticleData(
    override val title: String,
    override val body: String,
) : Article

internal data class ArticleMutator(
    override var title: String,
    override var body: String,
) : Article.Mutator
```

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
    val title: String
    val body: String

    interface Mutator : Article {
        override var title: String
        override var body: String
    }

    companion object {
        fun create(title: String, body: String): Article =
            ArticleData(title = title, body = body)
    }
}
```

### Accessing a Mutator

`mutate()` is **not** a method on the base interface. It is exposed as an extension function co-located with the entity's implementation files. This keeps the mutation path out of the entity contract and limits visibility to layers that legitimately need it.

```kotlin
// ArticleExtensions.kt — same package as ArticleData / ArticleMutator
fun Article.mutate(): Article.Mutator = ArticleMutator(
    title = this.title,
    body = this.body,
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


## Serialisation

The core domain model has no knowledge of serialisation formats (JSON, Protobuf, etc.). Annotations such as `@JsonProperty` or `@Serializable` must not appear on entity interfaces or their implementations.

Serialisation is the responsibility of the infrastructure layer. Infrastructure adapters translate between domain model types and wire formats using dedicated DTO classes or custom serialisers.

This boundary is enforced at the architecture level: `backend-core` must not depend on any serialisation library.
