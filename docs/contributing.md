# Contributing

## Repository Kotlin standards

### File and type structure

- Default to one top-level production type per file.
- Name each production file after its primary type or responsibility.
- The default exception bar is high. Only keep multiple production declarations together when the declarations are tightly coupled, locally obvious, and splitting them would reduce clarity rather than improve it.
- Keep file-private helpers and top-level declarations narrow and obviously coupled to the owning file. If the relationship is not immediate, move the type to its own file.
- Split files once they begin mixing orchestration, parsing, validation, rendering, diagnostics, policy, or I/O concerns.
- Avoid `util`, `misc`, and catch-all helper files. Prefer domain-led packages and names.

### Responsibility boundaries

- Keep orchestration separate from parsing, validation, rendering, diagnostics, and side-effecting I/O.
- Keep pure transformations separate from filesystem, process, environment, network, or resolver access.
- Model domain states and failure modes with Kotlin types instead of loosely coupled strings, maps, and boolean combinations.
- Prefer constructor injection for required collaborators and keep side-effecting dependencies explicit.
- Prefer composition over inheritance and keep collaborators explicit.
- Apply single-responsibility rigor to both files and classes. If a reviewer cannot summarize the unit in one sentence, the unit is probably too broad.

### Kotlin idioms

- Prefer immutable data, `val`, and expression-oriented control flow by default.
- Prefer guard clauses and early returns for preconditions, invalid state handling, and fast exits when they reduce nesting and make the main path easier to read.
- Default to the narrowest visibility that keeps the API honest. Widen visibility only when a real caller or extension point requires it.
- Omit explicit property types when an inherited contract already fixes the type or when the initializer is unambiguous and self-describing, such as `Foo::class`, a matching concrete constructor call, or a simple literal on an implementation override.
- Keep explicit types when they define a public contract, prevent ambiguous inference, stabilize a non-obvious declaration, or materially improve readability.
- Use `sealed interface`, `data object`, `value class`, exhaustive `when`, and null-safety where they make state and invariants clearer.
- Use extension functions when they improve discoverability for a well-scoped domain operation and avoid creating utility dumping grounds.
- Use infix functions only for DSL-facing APIs when readability is clearly better than the non-infix equivalent.
- Use `object` and `companion object` only when singleton semantics, namespaced factories, or constants are genuinely clearer than top-level declarations or regular types.
- Avoid Java-style static utility patterns, unnecessary mutable state, and scope-function chains that hide control flow.
- Do not contort control flow to appease return-count tooling. Guard clauses are allowed; excessive non-guard branching should still be simplified or decomposed.

### Interfaces and ports

- Introduce interfaces at meaningful boundaries such as filesystem access, process execution, environment access, diagnostics emission, dependency resolution, or other external integrations.
- Use interfaces when multiple implementations or isolation in tests materially improve design clarity.
- Do not add interfaces for trivial data holders or single concrete types where the extra indirection adds no value.

### Comments and KDoc

- Document invariants, contracts, ordering guarantees, and non-obvious behavior.
- Keep comments concise and durable. If a comment merely restates the code, remove it.
- Add KDoc for public DSL surfaces, scripting contracts, and non-obvious extension points where contributor intent would otherwise be unclear.

### Review and PR slicing rules

- Keep quality refactors behavior-preserving. If a change affects user-visible behavior or a public contract, split it into a separate issue and PR.
- Slice PRs by module or responsibility boundary, not by repository-wide search-and-replace.
- Move and rename types first, then simplify logic, then tighten tests. Do not blend unrelated cleanup into one diff.
- Add or update regression coverage next to the boundary being extracted.
- Automated structural guardrails are intentionally narrow and high-signal:
  - default to one non-private top-level production declaration per file
  - keep production Kotlin files at or below the configured line thresholds
  - require explicit package declarations for production Kotlin sources
  - keep package declarations aligned with `src/main/kotlin` directory paths
  - keep single top-level production declaration files named after the owning declaration
  - do not introduce `util`, `utils`, or `misc` package segments in production code
- If a structural exception is genuinely clearer than splitting the code, encode the exception in the repository quality policy with a narrow path-specific rationale and call it out in the PR description.
- Broader architecture and layering decisions remain review-gated. If a rule cannot be automated without becoming brittle, explain the boundary explicitly in review.
- Reviewers should check:
  - file ownership and package placement are obvious
  - one-top-level-production-type-per-file remains the default
  - orchestration and side effects are separated from pure logic
  - Kotlin features improve clarity rather than novelty
  - `verifyRepositoryStandards`, `detekt`, `ktfmtCheck`, and relevant tests remain green

## Build, test, and quality gates

Build the repository:

```bash
./gradlew clean build
```

Run tests:

```bash
./gradlew kotest
```

Run integration and end-to-end tests:

```bash
./gradlew integrationTest e2eTest
```

Unit tests live in `src/test`. Integration tests live in `src/integrationTest`. End-to-end tests live in `src/e2eTest` and skip themselves when their toolchain is absent.

Run static analysis and check Kotlin formatting:

```bash
./gradlew detekt ktfmtCheck
```

Run repository structural guardrails directly:

```bash
./gradlew verifyRepositoryStandards
```

Format Kotlin sources:

```bash
./gradlew ktfmtFormat
```

Useful notes:

- the Gradle build is configured for Java 24
- `./gradlew build` includes the root `check` lifecycle and therefore runs `verifyRepositoryStandards`
- if `ktfmtCheck` fails, run `./gradlew ktfmtFormat` and rerun checks
- if `detekt` fails, inspect the generated report under `build/reports/detekt/`
- if `verifyRepositoryStandards` fails:
  - split extra production types into their own files or make tightly coupled helpers private
  - split large production files by responsibility before raising any threshold
  - add or correct explicit package declarations and keep them aligned with `src/main/kotlin` paths
  - rename single-type files so the file name matches the owning declaration
  - only add a path-specific exception in the repository quality policy when the split would genuinely reduce clarity, and explain that exception in the PR

[Back to the README](../README.md)
