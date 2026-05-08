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

### Simplicity Is Best

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


### Frontend Library Build Optimisation

These rules apply to any frontend module that is published as a library (i.e. has a `build.lib` entry in its Vite config).

- **Declare peer dependencies correctly.** Any package that the consuming application is expected to supply — typically heavy runtime dependencies such as UI frameworks, editor cores, or syntax-highlighting engines — must be listed in `peerDependencies`, not `dependencies`. Also list them in `devDependencies` so the library's own development environment installs them.
- **Externalise peer dependencies in the bundler.** Packages listed in `peerDependencies` must be excluded from the library's bundle output. In Vite, configure `build.rollupOptions.external` to match every such package (including subpath imports such as `highlight.js/lib/languages/*`). Bundling a peer dependency causes duplicate code in the consumer's final build and inflates the library's own dist file.
- **Enable minification explicitly.** Set `build.minify: 'esbuild'` in `vite.config.ts`. Do not rely on the bundler default, which may change across versions.
- **Verify output size after every build change.** Record the gzip size reported by `vite build` and confirm it has not regressed unexpectedly before committing.


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
