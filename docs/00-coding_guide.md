# Hwan's Coding Guide

A collection of useful coding guidelines from Hwan's experience.

This guide emphasizes carefulness over speed. However, for simple tasks, use your judgment as appropriate.

Written for both humans and LLMs.


## Core Rules

Rules are listed in order of importance. Violating these rules can cause serious problems, so be sure to follow them.

1. When coding with AI: Do not add AI attribution to any output (code, commits, PRs, documents, comments, etc.).
2. If you judge that existing code needs to be modified or deleted for any of the following reasons, always follow an explicit confirmation process:
   - Previously passing tests are now failing
   - Logic you believe is no longer needed
3. Before making any architecture-related decisions, always follow an explicit confirmation process with a detailed explanation of your reasoning.


## Coding Guide

The following are recommendations for code quality and consistency.


### Coding Style

- Changes should be clearly explained through code, not comments.
- Do not use magic literals. Declare them as constants and reference those constants.
- Limit line length to a maximum of 120 characters for alphanumeric-only content, and 60 characters when CJK characters are included.
- When writing comments, follow the style guide below. Adjust comment characters to match each programming language's primary style as needed. `{}` in the style guide below represents a placeholder — replace it with the appropriate content.
  * File-level comments:
    ```
    {filename}

    {$Since: file creation date - ISO 8601 format, UTC}
    ```
  * Class/interface/protocol-level comments:
    ```
    {Summary of functionality in at most 1 paragraph}

    {@deprecated: (if needed) preserved for backward compatibility, scheduled for removal}
    {@author: author name}
    {@since: version of initial introduction}
    {@version: current version}
    ```
  * Function/method/subroutine-level comments:
    ```
    {Summary of functionality in at most 1 paragraph}

    {@deprecated: (if needed) preserved for backward compatibility, scheduled for removal}
    {@param: list of parameters and descriptions}
    {@return: (if needed) return value}
    {@exception: (if needed) list of possible exceptions}
    {@since: version of initial introduction}
    {@version: current version}
    ```


### Think Before You Code

- Do not rush to code.
- Do not guess.
- Do not hide ambiguities.
- State your assumptions explicitly.
- When multiple interpretations are possible, present all options rather than making an implicit choice.
- Clearly articulate the trade-offs of the chosen solution.
- Choose the simpler approach when available.
- Push back when appropriate.
- Stop when something is unclear.
- Identify what is ambiguous or uncertain, and ask.
- Verify that the problem you are solving is the *real* problem. Find the root cause, not the symptom.


### The Importance of Naming

- A name alone should tell you what it does.
- Avoid meaningless names like data, result, temp, info.
- This is not a TOEFL or IELTS exam. Be forgiving of minor mistakes.
- Use clear, full words over abbreviations (with conventional exceptions: i, ctx, req).
- Booleans should start with is, has, or can.
- Function names should start with a verb; class/object names should start with a noun.


### Plan in Detail

- Think through the big-picture steps first, then use smaller sub-steps to support those larger ones.
- Visualize the plan using the following indented format:
  ```
  Step 1: xxxx
    Step 1-1: yyyy
    Step 1-2: zzzz

  Step 2: xxxx
    Step 2-1: yyyy

  Step 3: xxxx
  ...
  ```

### Code Is a Liability

Every line of code carries a cost. It must be read, understood, tested, maintained, and eventually deleted. The more code a codebase contains, the heavier the burden on everyone who works with it. Writing less code is always preferred over writing more. Before adding any new code, ask whether the same goal can be achieved by removing or reusing existing code instead.

### Simplicity Is Best

- Before writing any static helper, utility, or shared function, search the codebase for an existing equivalent. Reuse before creating.
- Write only the minimum amount of code needed to solve the problem.
- Do not include speculative code.
- Do not add features beyond what was requested.
- Do not apply abstractions to one-off code.
- Do not add unrequested "flexibility" or "configurability."
- Do not handle error cases for impossible scenarios.
- If you wrote 200 lines but can reduce it to 50, rewrite it.
- Ask yourself whether someone reading it 6 months from now — including future you or a colleague — will understand it.
- Choose boring but clear code over clever code.
- Ask yourself: "Would a senior engineer say this code is overly complex?" If yes, simplify it.


### Keep Changes Minimal

- Only touch what is absolutely necessary.
- If there are fewer than three reasons why this change is needed, hold off.
- Do not "improve" adjacent code, comments, or formatting near what you are changing.
- Honestly assess whether this is a refactor or a rewrite. If it is a rewrite, don't do it.
- Follow the style of the existing codebase.
- Remove any imports/includes/variables/functions that you made unused through your changes.
- Unless explicitly asked, leave existing unused code as-is if you discover it.
- Before making changes, verify that all existing tests pass. This lets you distinguish between what your change broke and what was already broken.
- Verify that your changes comply with the tests.


### Commit Units and Rules

- Each commit should have a single logical reason.
- A commit message should capture *why* you did it, not just *what* you did.
- Each commit must be self-contained — i.e., it must build and all tests must pass.
  * However, there may be changes where this rule cannot be applied. In such cases, you must be able to clearly explain why.
- The larger the commit, the higher the review cost and the harder it is to trace issues. Keep commit size considerate of the reviewer's reading burden.
- Do not mix feature changes and formatting cleanup in a single commit. Doing so makes it impossible to distinguish intentional changes from incidental ones.
- Read the diff yourself before committing. Verify that only the changes you intended are included.
- Use the imperative mood and do not end with a period. The subject line is limited to 72 characters for alphanumeric-only content and 36 characters when CJK characters are included. No limitations for commit message body - feel free to compose commit message body. Don't apply characters limit for the commit message body!


### Dependency Management

- Before adding a new library, check whether the standard library or an already-used dependency can solve the problem.
- Do not introduce a large library for a single feature. It may be better to implement only the needed functionality yourself.
- Check that the license of the library you are adding does not conflict with your project.
- Periodically check the maintenance status of your dependencies. A library whose last commit was years ago is a warning sign.
- Pin dependency versions explicitly. Implicitly following the latest version leads to unpredictable behavior.
- When removing a dependency, verify all usage sites connected to that library and clean up any related code.
- Every package you import directly must appear in `dependencies` or `peerDependencies`. Relying on a transitive dependency is fragile and will break if the intermediary package changes.


### Tests: Living Specification Documents

- Tests should clearly express the specification and validation goals of the implementation.
- Define success criteria and iterate until they are validated.
- Validation follows the structure of: "input, output, comparison with expected result."
- Convert tasks into verifiable goals:
  * "Add validation" → "Write tests for invalid input and make them pass"
  * "Fix a bug" → "Write a test that reproduces the bug and make it pass"
  * "Refactor X" → "Confirm the tests pass both before and after the refactor"
- The name, title, or description of a test should follow the format `xxx should yyy when zzz`. Use this format to describe the problem or specification. In this format, 'xxx', 'yyy', and 'zzz' have the following meanings:
  * 'xxx': The system under test.
  * 'yyy': The expected result.
  * 'zzz': The given condition.
- When using a test framework that supports nested structures, use nesting to avoid redundancy in descriptions as much as possible. Example:
  ```
  describe("PasswordValidator:", () => {
    describe("validateLength function:", () => {
      it("returns false for input shorter than ${SysConfig.PASSWORD_LENGTH} characters", () => { ... });
      it("returns true for input of ${SysConfig.PASSWORD_LENGTH} characters or more", () => { ... });
    })
  });
  ```
- When a single test subject (`xxx`) has two or more cases, always group them under a nested block. Two or more tests that share the same `xxx` but differ only in `yyy` or `zzz` must not sit flat at the same level — use `@Nested` inner class (JUnit 5 / Kotlin), `context`/`describe` block (Spock, Jest), or the equivalent construct in the test framework being used.

#### Common Principles for All Tests

- Isolation: Tests must operate independently regardless of execution order. Do not depend on state left by other tests. When this principle is upheld, parallel execution becomes possible.
- Determinism: Given the same input, tests must always return the same result.

#### Test Size Classification

Test sizes are defined as Small, Medium, and Large.

- Size determination tree:

  ```
  Q1. Does the test use external dependencies such as network, filesystem, DB, or external processes?
      → No  : Small
      → Yes : Go to Q2

  Q2. Can the test directly initialize and control that dependency?
      → Yes : Medium (apply the dependency allowance table for the final decision)
      → No  : Large
  ```

- Decision examples:

  | Situation | Size |
  |---|---|
  | Validating UserService logic alone, DB replaced by Mock | Small |
  | Validating UserService + local H2 DB integration | Medium |
  | Validating UserService + actual PostgreSQL + external payment API | Large |
  | Single class test but uses Thread.sleep() internally | Medium |
  | Two-component interaction test but no external dependencies | Small |

##### Small Test

- Commonly called a **Unit Test**, but more precisely defined by constraints, not scope. Testing a single class with file I/O or sleep is not Small.
- Verifies the completeness of the smallest logical unit.
- Does not use network access, filesystem, or database.
- Isolates external dependencies as much as possible. Actively use test doubles such as Dummy, Mock, Spy, and Stub.
- Runs most frequently and must maintain fast execution speed.
- Target an execution time of 50ms or less per single logic unit.

##### Medium Test

- Commonly called an **Integration Test**, but testing the interaction of two components does not qualify as Medium if no network, filesystem, or DB is used — that would be Small.
- Covers the full scope of Small Tests.
- Replace dependencies whose results cannot be verified with test doubles.
- Dependencies that can be directly controlled within the local system and whose results can be verified through logic are allowed.
- Dependency allowance table:

  | Dependency | Allowed | Reason |
  |---|---|---|
  | Filesystem | Allowed | The test can directly create and control a temporary path, and results can be verified through logic |
  | Printer | Not allowed | You can confirm that something was printed, but cannot verify the output through logic |
  | Database | Conditionally allowed | Allowed only for locally managed instances that the test directly initializes and controls. Shared external DBs are not allowed |
  | Network (external server) | Not allowed | Responses are not guaranteed to behave as expected; the test becomes dependent on external service state |
  | Network (localhost loopback) | Conditionally allowed | Allowed only for servers directly started and controlled by the test |
  | In-process Cache (e.g., Caffeine) | Allowed | Fully controllable within the process and results can be verified |
  | External Cache (e.g., Redis) | Conditionally allowed | Allowed only for locally managed instances that the test directly initializes and controls. Shared external instances are not allowed |
  | Message Queue (e.g., Kafka) | Conditionally allowed | Allowed only for locally controlled instances. Due to async nature, take care with timing of result verification |
  | External process / CLI tool | Conditionally allowed | Allowed when installed locally and results can be verified through logic. Be aware of version dependency issues |
  | System Clock | Conditionally allowed | Direct use is not allowed. Must be abstracted (e.g., via a Clock interface) to be controllable |
  | Environment variables / System properties | Allowed | Can be set and restored within the test. Must be isolated between tests to prevent contamination |
  | External API (HTTP, etc.) | Not allowed | Responses are not guaranteed to behave as expected; the test becomes dependent on external service state |

- Decision criteria for dependencies not in the table:

  When a new dependency appears, apply the following three questions in order:

  ```
  Q1. Can the test directly initialize and control this dependency?
      → No  : Not allowed
      → Yes : Go to Q2

  Q2. Can the result of the dependency's behavior be verified through logic?
      → No  : Not allowed
      → Yes : Go to Q3

  Q3. Does the state of the dependency affect other tests? (Can it be isolated?)
      → No (affects others) : Not allowed, or replace with a test double
      → Yes (isolated)      : Allowed
  ```

- **Full-context boundary:** A test that starts the full Spring application context — for example via `@SpringBootTest(webEnvironment = RANDOM_PORT)` — is classified as **Large** even when every dependency (database, loopback server) is locally controlled. Full context startup exercises the entire wiring, security configuration, and migration stack; this fidelity belongs to the Large Test category. Apply the Q1/Q2 tree only to partial-context tests such as `@WebMvcTest` or `@DataJpaTest`.

##### Large Test

- Commonly called an **End-to-End Test**, but tests like performance tests or chaos engineering that do not validate full scenarios still qualify as Large if they use real external dependencies.
- The core purpose of a Large Test is to verify that the entire system behaves correctly under conditions as close to actual production as possible. Environment fidelity takes priority over scope.
- Minimize the use of test doubles and maintain dependencies on external systems where possible. Use the following criteria:
  ```
  1. Can you actually access the external system? → Use the real thing if possible
  2. Is access unavailable? (restrictions, cost, security, etc.) → Test doubles allowed
  3. Is there a risk of being blocked due to repeated access? → Test doubles allowed
  ```
  When using test doubles, base their behavior on the latest official specification documentation of that system.
- Core values that Large Tests must uphold:
  * Fidelity: Implement so that defects undetectable by unit tests — such as test doubles in Small/Medium tests behaving differently from real implementations, configuration errors, and load scenarios — can be discovered.
  * Minimum test scope: The higher the fidelity, the greater the cost and instability. Design Large Tests to target as narrow a System Under Test (SUT) as possible. Multiple small Large Tests are better than one giant Large Test.
  * Accepting non-determinism: Large Tests can produce different results under the same conditions due to external dependencies. Do not ignore intermittent failures (flaky tests) — identify the root cause and isolate it, or explicitly define the acceptable range.
  * Privacy compliance: When using or copying production data, be careful not to include personally identifiable or other sensitive data.


### Package Structure

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


### Kotlin-specific Conventions

- `companion object` blocks must appear at the **bottom** of a class body. This matches the [official Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html#class-layout).


### Backend Layer Conventions

#### Controller Package Separation

View controllers (serving HTML/templates) and REST API controllers must reside in separate, clearly named packages. Mixing them in a single package creates ambiguity as the surface area grows.

| Package | Contents |
|---|---|
| `{root}.view` | `@Controller` classes that return views or `ModelAndView` |
| `{root}.api.endpoint.{domain}` | `@RestController` interfaces and their implementations |

#### REST API Controller Design

Every REST API controller must be defined as an interface and implemented in a separate `impl` sub-package. This separation enables clean OpenAPI documentation annotations on the interface, keeps the implementation free of documentation noise, and makes the contract explicit.

```
{root}.api.endpoint.{domain}/Controller.kt      — interface (annotations, contract)
{root}.api.endpoint.{domain}/impl/ControllerImpl.kt — @RestController implementation
```

Rules for the interface:
- Declare all `@RequestMapping`, `@Operation`, `@ApiResponse`, and `@Tag` annotations here.
- Do not reference implementation details.

Rules for the implementation:
- Annotate the class with `@RestController` and mark it `internal`.
- Override every method from the interface; add no extra public surface.
- Keep all Spring-documentation annotations off the implementation class.

#### API Versioning

Expose all REST API endpoints under a version prefix in the URL path. The current version prefix is `/v1`. Example: `/api/v1/users`.

When a new major version is introduced, add a new prefix (e.g., `/v2`) rather than modifying existing versioned paths.

#### API Documentation

Use **Springdoc-OpenAPI** as the API documentation tool. Annotations belong on the controller interface, not on the implementation. Annotate every endpoint with `@Operation` and `@ApiResponse`; annotate every controller interface with `@Tag`.

#### DTO Package Layout

Request, response, and shared DTO types for a controller domain must be organised under a dedicated `dto` sub-package:

| Package | Contents |
|---|---|
| `{endpoint}.dto` | Shared/common DTO types |
| `{endpoint}.dto.request` | Request DTOs |
| `{endpoint}.dto.response` | Response DTOs |


### Static Analysis

Use **Detekt** for Kotlin static analysis. Configure it to run before the `compileKotlin` task so that violations are caught at build time.

- Address every Detekt finding. Prefer fixing the root cause over suppressing.
- When suppression is unavoidable, use `@Suppress("RuleId")` with a 1–2 line comment directly above it explaining the reason. A suppression without explanation is not acceptable.


### Utility Conventions

A utility is a stateless, context-free, pure function (or a group of closely related pure functions) that can be called from anywhere without external setup.

- Place utilities in a dedicated `util` package within the core module (e.g., `{core}.util`).
- Group related utilities into sub-packages by concern: `{core}.util.io`, `{core}.util.math`, etc. Utilities that do not fit a named concern live directly in `{core}.util`.
- Before writing a new utility, check whether an equivalent already exists. Reuse before creating.
- Before extending or modifying an existing utility, discuss the rationale. Utilities carry high dependency weight — a change affects every call site.
- Every utility must have exhaustive Small Tests that serve as its living specification.

**Utility & helper indexes** — consult these before writing any new helper code, and keep them up to date whenever a utility is added, changed, or removed:

- Backend: [`docs/02-coding_guide_utilities_and_helpers_backend.md`](02-coding_guide_utilities_and_helpers_backend.md)
- Frontend: [`docs/02-coding_guide_utilities_and_helpers_frontend.md`](02-coding_guide_utilities_and_helpers_frontend.md)


### Infrastructure Layer Testing

Any layer that integrates with infrastructure concerns — including repositories, persistence adapters, and REST API controllers wired into a real application context — must be covered by both Small Tests and Medium Tests.

- **Small Tests** verify logic in isolation using test doubles.
- **Medium Tests** verify the integration with the actual infrastructure component (a real database, a real HTTP stack via `@WebMvcTest`, etc.).

For REST API modules, at least one Spock `@SpringBootTest` Large Test (smoke test) must be written per controller group. The smoke test must verify at least one happy-path scenario end-to-end with a fully loaded Spring context. Exhaustive case coverage — boundary conditions and error paths — belongs in Small and Medium Tests, not in the smoke test.

#### Large Test Utility Conventions

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
