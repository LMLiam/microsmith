# Microsmith

Microsmith is a small Kotlin DSL framework for declaring and composing domain-specific model extensions (schemas, services, etc.). It provides a lightweight immutable model produced by DSL blocks, and extension points for plugin authors to add domain-specific DSLs (for example, schema dialects like Protobuf or JSON).

This repository is a multi-module Gradle project containing:

- `dsl` – Core DSL primitives and helpers (the entrypoint `microsmith { ... }`, model, builder, and helper extensions).
- `dsl-schemas` – A schema DSL extension which provides `schemas { ... }` block and core schema types.
- `kotest` – Project-wide Kotest configuration used by the test suites.

## Key features

- Minimal, immutable `MicrosmithModel` produced by the `microsmith {}` DSL entrypoint.
- Plugin-friendly extension model: plugins can attach `MicrosmithExtension` implementations which are discoverable from the model.
- Example extension: `dsl-schemas` demonstrates how to collect and query schemas via a `SchemasExtension`.

## Quickstart — build & test

This project uses the Gradle wrapper. From the repository root run:

```bash
./gradlew clean build
```

To run tests only:

```bash
./gradlew kotest
```

Notes:
- Kotlin JVM toolchain is configured to use Java 24 in the root Gradle configuration.
- Tests use Kotest (v6) and the project defines a `KotestConfig` to emit JUnit XML results into the Gradle build directory.

## Usage example

Here's a tiny snippet demonstrating the public DSL API provided by the core `dsl` module:

```kotlin
import me.liam.microsmith.dsl.core.microsmith

val model = microsmith {
    // DSL blocks go here, e.g.:
    // schemas { ... }  // provided by a dialect plugin
}

// Read an extension:
val ext = model.get<YourExtensionType>()
```

## Publishing

The root Gradle config configures `maven-publish` and a GitHub Packages repository at `https://maven.pkg.github.com/lmliam/microsmith`.
To publish, set `GITHUB_ACTOR` and `GITHUB_TOKEN` environment variables (or provide `gpr.user` / `gpr.key` Gradle properties) and run:

```bash
./gradlew publish
```

## Testing locally

- `./gradlew clean build` — full build and tests
- `./gradlew kotest` — run Kotest tasks specifically