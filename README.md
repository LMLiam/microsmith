# Microsmith

Microsmith is a Kotlin DSL and standalone CLI for declaring domain-specific models and generating artifacts from `.microsmith.kts` scripts.

It generates Protobuf schemas, Protobuf RPC definitions, and ASP.NET services. It runs inside consumer Gradle and Maven builds, and from standalone repositories in any language.

## Why

A model that lives in a generator drifts from the model that lives in the codebase. Microsmith makes the script the only source of truth:

- you declare schemas and services in Kotlin
- five explicit stages turn the model into resolved models, artifacts, and files
- every generated file records its origin in `.microsmith/origins.json`

The pipeline is a set of interfaces, so a new output family is a module on the classpath. The core does not need to know that the module exists.

## Quick start

Manual installs require Java 24 or newer. The installers provision a runtime when the machine has none.

### Standalone CLI

```bash
VERSION=<microsmith-version>
curl -fsSL -o microsmith-install.sh "https://github.com/LMLiam/microsmith/releases/download/v${VERSION}/microsmith-install.sh"
sh microsmith-install.sh --version "${VERSION}"

microsmith init
microsmith run build.microsmith.kts
```

`microsmith init` detects the repository profile and writes `build.microsmith.kts` and `settings.microsmith.kts`. It does not overwrite an existing script unless you pass `--force`. Protobuf output lands in `./proto`.

### Gradle

```kotlin
plugins {
    id("io.github.lmliam.microsmith") version "<version>"
}

microsmith {
    scriptFile.set(layout.projectDirectory.file("schema.microsmith.kts"))
}
```

```bash
./gradlew microsmithGenerate
```

### Maven

```xml
<plugin>
    <groupId>io.github.lmliam.microsmith</groupId>
    <artifactId>maven-plugin</artifactId>
    <version>${microsmith.version}</version>
</plugin>
```

```bash
mvn microsmith:generate
```

## The DSL

```kotlin
microsmith {
    schemas {
        protobuf {
            message("UserCreated") {
                int32("id") { index(1) }
                string("email") { index(2) }
            }

            service("UserService") {
                "GetUser" {
                    "GetUserRequest" to "GetUserResponse"
                }
            }
        }
    }
}
```

A script can also call `emit(model)` or `generate(model)` to control the output root.

## Documentation

| Document | Contents |
|---|---|
| [Installation](docs/install.md) | Install paths, checksums, manual channels |
| [Command line interface](docs/cli.md) | `init`, `run`, `doctor`, `ide`, diagnostics, exit codes |
| [DSL reference](docs/dsl.md) | Core DSL, Protobuf schemas, ASP.NET services |
| [Gradle plugin](docs/gradle.md) | Task wiring, generated sources, IDE behaviour |
| [Maven plugin](docs/maven.md) | Goal wiring, generated sources, IDE behaviour |
| [JetBrains IDE helper](docs/ide.md) | Helper project, fallback jar, when to use each |
| [Plugin resolution](docs/plugins.md) | Resolution, checksums, credentials, cache |
| [Architecture](docs/architecture.md) | Pipeline stages, modules, boundaries |
| [Contributing](docs/contributing.md) | Kotlin standards, build and quality gates |
| [Releases](docs/release.md) | Fixtures, validation matrix, distribution |
| [Troubleshooting](docs/troubleshooting.md) | Common failures and their causes |
| [CI examples](docs/ci-examples.md) | Copy-ready workflows |

The CLI usage text has one source: run `microsmith --help`.

## Repository layout

- `modules/`: production Gradle subprojects grouped into pipeline families (`dsl-*`, `resolve-*`, `artifact-*`, `compile-*`, `gen`) and execution layers (`runtime-scripting`, `cli`, `gradle-plugin`, `maven-plugin`)
- `build-logic/`: included Gradle build that owns repository plugins and quality tasks
- `examples/`: consumer-repository fixtures
- `docs/`: user and contributor documentation
- `gradle/`: wrapper files, version catalog, and dependency locks
- `.github/`: workflows and repository automation

## Build and test

```bash
./gradlew build            # compile, test, and run every quality gate
./gradlew kotest           # run the test suites
./gradlew detekt ktfmtCheck
./gradlew ktfmtFormat      # rewrite formatting
```

`./gradlew build` includes `verifyRepositoryStandards`, which checks file naming, package layout, and import rules.

## Contributing

Read [docs/contributing.md](docs/contributing.md) before you change code. It covers the Kotlin standards, the quality gates, and the test policy.

## Licence

Apache License 2.0. See [LICENSE](LICENSE).
