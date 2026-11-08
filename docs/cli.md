# Command line interface

## Standalone CLI for consumer repositories

The CLI is the recommended entrypoint for Go, .NET, Node, Python, Ruby, Rust, build-tool-light JVM repositories, and any repository where you want self-contained Microsmith execution without depending on a local Gradle or Maven install.
The official installer scripts are self-contained and provision a Java 24 runtime automatically when the machine does not already provide one.

Manual channels remain available, but they require Java 24 or newer.

### Recommended bootstrap flow

Install Microsmith, then initialize the repository and run generation:

```bash
microsmith init
microsmith run build.microsmith.kts
```

`microsmith init` is deterministic and non-destructive by default:

- it detects Node, Go, Java, Kotlin, Python, Ruby, Rust, and .NET repositories from `package.json`, `go.mod`, Java source roots such as `src/main/java` or `src/test/java`, Kotlin source roots such as `src/main/kotlin`, `src/test/kotlin`, or `src/commonMain/kotlin`, root JVM build markers such as `pom.xml`, `build.gradle`, `build.gradle.kts`, `settings.gradle`, `settings.gradle.kts`, `pyproject.toml`, `requirements.txt`, `setup.py`, `setup.cfg`, `Gemfile`, `gems.rb`, a root `*.gemspec`, a root `Cargo.toml`, `*.csproj`, and `*.sln`, and otherwise falls back to a generic bootstrap
- repositories that match multiple first-class profiles at once, including mixed Java and Kotlin source roots, fall back to the generic bootstrap instead of guessing the dominant ecosystem
- Java detection is intentionally conservative: it requires a Java source root either at the repository root or inside a nested Maven or Gradle module, then adds root Maven or Gradle markers when present; build-file-only JVM roots stay on the generic path so Microsmith does not guess between Java and Kotlin from build files alone
- Kotlin detection is source-root driven: it recognizes root or nested Kotlin source roots for Maven, Gradle Kotlin DSL, Gradle Groovy, multiplatform-style source-set layouts, and build-tool-light repositories; build-file-only JVM roots still stay on the generic path
- Ruby detection covers Bundler-first app roots and gem-style repositories through a root `Gemfile`, root `gems.rb`, or root `*.gemspec`; nested gems without a root Ruby marker stay on the generic path
- Rust detection covers both package manifests and workspace roots through a root `Cargo.toml`; nested crates without a root manifest stay on the generic path
- it creates `settings.microsmith.kts` when missing
- it creates `build.microsmith.kts` when missing
- it preserves existing regular bootstrap files instead of overwriting them
- it refreshes `.microsmith/ide/*` helper metadata by default
- it prints configured assets and the exact next generation command to run

Important behavior:

- if you pass `--repo-root <path>`, that directory must already exist
- pass `--force` when you want the managed bootstrap scripts to replace existing regular files
- pass `--skip-ide-helper` when you do not want `.microsmith/ide/*` generated during `init`
- if you skip IDE helper generation, `microsmith doctor` will continue to report bootstrap as incomplete until you run `microsmith ide refresh`
- after a successful `init`, no additional IDE command is required for the default path

If your repository has a deliberate alternative output root, override `--out` explicitly:

- Node: `microsmith run build.microsmith.kts --out ./generated`
- Go: `microsmith run build.microsmith.kts --out ./internal/gen`
- Java: keep the default repository-root `./proto` output path unless you explicitly wire generated sources into Maven or Gradle later
- Kotlin: keep the default repository-root `./proto` output path unless you explicitly wire generated sources into Gradle or Maven later
- Python: keep the default repository-root `./proto` output path unless your repository already has a stronger convention
- Ruby: keep the default repository-root `./proto` output path unless your repository already has a stronger convention
- Rust: keep the default repository-root `./proto` output path unless your repository already has a stronger convention
- .NET: `microsmith run build.microsmith.kts --out ./Generated`

Java-specific guidance:

- Maven-based Java repositories: prefer the [native Maven integration](maven.md) when you want imported-project IDE support and Maven-goal execution
- Gradle-based Java repositories: prefer the [native Gradle integration](gradle.md) when you want imported-project IDE support and Gradle-task execution
- build-tool-light Java repositories: use the same default repository-root `./proto` output path and helper workflow

Kotlin-specific guidance:

- Gradle Kotlin DSL repositories: prefer the [native Gradle integration](gradle.md) when you want imported-project IDE support and Gradle-task execution
- Gradle Groovy repositories that primarily host Kotlin code: the same native Gradle plugin path applies when Kotlin is the primary source language
- Maven Kotlin repositories: prefer the [native Maven integration](maven.md) when you want imported-project IDE support and Maven-goal execution
- build-tool-light Kotlin repositories: use the same default repository-root `./proto` output path and helper workflow

### Direct script execution

Create a script such as `schema.microsmith.kts`:

```kotlin
microsmith {
    schemas {
        protobuf {
            message("UserCreated") {
                int32("id") { index(1) }
                string("email") { index(2) }
            }
        }
    }
}
```

Run generation:

```bash
microsmith run schema.microsmith.kts
```

Pass script context values when needed:

```bash
microsmith run schema.microsmith.kts --var env=prod --flag emit
```

Run in a separate JVM for stronger isolation:

```bash
microsmith run schema.microsmith.kts --isolation process
```

## Command reference

Run `microsmith --help` for the current usage text and environment variables.

### Canonical happy paths

Local:

```bash
microsmith init
microsmith run build.microsmith.kts
```

CI:

```bash
microsmith init --diagnostics json --verbose
microsmith run build.microsmith.kts --diagnostics json --verbose
```

Maintenance:

```bash
microsmith doctor --diagnostics json --verbose
microsmith ide doctor --diagnostics json --verbose
microsmith ide refresh
```

### What each command does

`microsmith init`

- bootstraps `settings.microsmith.kts` and `build.microsmith.kts`
- detects Node, Go, Python, Ruby, Rust, .NET, or generic repository shape and emits a matching starter script
- refreshes `.microsmith/ide` by default
- preserves existing regular bootstrap files unless `--force` is provided
- supports `--repo-root`, `--force`, `--skip-ide-helper`, `--diagnostics`, and `--verbose`

`microsmith run`

- executes a `.microsmith.kts` script and writes generated files under the configured output root, which defaults to the repository root and emits `.proto` files under `./proto`
- requires the script file to use the `.microsmith.kts` extension
- supports `--var`, `--flag`, `--plugin`, `--plugin-jar`, `--offline`, `--repository`, `--isolation`, `--diagnostics`, `--verbose`, and `--event-log`

`microsmith doctor`

- validates the runtime environment
- checks Java runtime availability, built-in provider discovery, script cache writability, plugin cache writability, repository policy initialization, and incomplete bootstrap state in the current working directory

`microsmith ide refresh`

- generates or refreshes `.microsmith/ide`
- is idempotent and only manages files under `.microsmith/ide`

`microsmith ide doctor`

- validates that the IDE helper exists and matches the active runtime classpath
- checks repository root validity, helper directory presence, required helper files, runtime classpath resolution, and helper classpath synchronization

### Diagnostics contract

When `--diagnostics json` is used, each event is emitted as one JSON line with:

- `timestamp`
- `level`
- `message`
- `code` for error events
- `details` when `--verbose` is enabled and details are available

### Exit codes

| Category            | Code          | Exit | Meaning                                        |
|---------------------|---------------|-----:|------------------------------------------------|
| Usage               | `MS-CLI-0001` |  `2` | Invalid command or flags.                      |
| Provider validation | `MS-CLI-1001` | `10` | Built-in provider validation failed.           |
| Plugin resolution   | `MS-CLI-1101` | `11` | Plugin resolution failed.                      |
| Script validation   | `MS-CLI-2001` | `20` | Script input validation failed.                |
| Script compilation  | `MS-CLI-2002` | `21` | Script compilation failed.                     |
| Script evaluation   | `MS-CLI-2003` | `22` | Script evaluation failed.                      |
| Script host         | `MS-CLI-2004` | `23` | Script host failed unexpectedly.               |
| Doctor              | `MS-CLI-3001` | `30` | `doctor` detected environment issues.          |
| IDE refresh         | `MS-CLI-4001` | `40` | `ide refresh` failed.                          |
| IDE doctor          | `MS-CLI-4101` | `41` | `ide doctor` detected issues or failed.        |
| Init conflict       | `MS-CLI-5001` | `50` | `init` detected conflicting filesystem state.  |
| Init validation     | `MS-CLI-5002` | `51` | `init` input or environment validation failed. |
| Init runtime        | `MS-CLI-5003` | `52` | `init` failed unexpectedly at runtime.         |

## Migration from Gradle

Keep the native Gradle plugin when the repository already uses Gradle and imported-project IDE support is part of the goal.
Use the CLI when you want self-contained execution outside Gradle, or when the repository does not carry a Gradle wrapper.
The DSL surface stays the same; the execution boundary changes.

### Command mapping

| Previous pattern                 | CLI replacement                       |
|----------------------------------|---------------------------------------|
| Gradle task invoking generation  | `microsmith run build.microsmith.kts` |
| Gradle-managed plugin dependency | `--plugin group:artifact:version`     |
| local classpath jar wiring       | `--plugin-jar ./path/to/plugin.jar`   |
| Gradle offline mode              | `--offline`                           |

### Recommended migration sequence to the standalone CLI

1. Keep the Microsmith DSL in `*.microsmith.kts` files.
2. Replace Gradle-based generation commands in CI with `microsmith run`.
3. Pin plugin coordinates and validate lock or checksum behavior for reproducibility.
4. Enable the relevant security controls for your environment.
5. Remove obsolete Gradle generation tasks after parity is proven.

[Back to the README](../README.md)
