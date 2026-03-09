# Microsmith

Microsmith is a Kotlin DSL and standalone CLI for declaring domain-specific models and generating artifacts from `.microsmith.kts` scripts.
It is designed to work both inside this Gradle repository and from consumer repositories that do not use Gradle, including Go, .NET, and Node projects.

This README is the canonical repository documentation.

## Contents

- Overview
- Repository modules
- Repository Kotlin standards
- Build, test, and quality gates
- DSL usage
- Standalone CLI for non-Gradle repositories
- Installation and verification
- Command reference
- JetBrains IDE helper
- Plugin resolution and security model
- CI examples
- Example fixtures
- Troubleshooting
- Migration from Gradle
- Distribution and release artifacts

## Overview

Microsmith provides:

- a core immutable model built from `microsmith { ... }`
- extension points for schema and generation dialects
- a standalone CLI for running `.microsmith.kts` scripts without embedding Gradle in consumer repositories
- bundled built-in providers for the default schema and protobuf workflows
- a JetBrains IDE helper workflow for stronger type resolution in non-Gradle repositories

## Repository modules

- `dsl`: core DSL primitives, builders, model types, and extension APIs
- `dsl-schemas`: generic schema registry and schema-oriented DSL surface
- `dsl-schemas-protobuf`: protobuf-flavoured schema DSL on top of `dsl-schemas`
- `gen`: generator contracts, model traversal, and shared generation helpers
- `gen-schemas`: schema-aware generation support on top of `gen`
- `gen-schemas-protobuf`: protobuf emitters, rendering, and protobuf-specific generation support
- `runtime-scripting`: Kotlin scripting host for `.microsmith.kts` execution
- `cli`: command-line entrypoint, diagnostics, installer packaging, and IDE helper support
- `kotest`: shared Kotest configuration used by repository test suites

### Module boundary map

- `dsl` is the foundational model and DSL layer. It should stay free of CLI, scripting-host, installer, resolver, and generation-application concerns.
- `dsl-schemas` extends `dsl` with schema registration and schema-oriented DSL concepts. It should not absorb CLI or runtime concerns.
- `dsl-schemas-protobuf` adds protobuf-specific schema modeling on top of `dsl` and `dsl-schemas`. Keep protobuf domain types, builders, and DSL entrypoints here rather than leaking them into application layers.
- `gen` owns generator contracts and shared generation abstractions. It should not contain CLI command handling, repository bootstrapping, or scripting host orchestration.
- `gen-schemas` and `gen-schemas-protobuf` own schema-aware emission, rendering, and validation. They should depend downward on model and generator layers, not upward on CLI or installer behavior.
- `runtime-scripting` is the scripting host and execution boundary. It may depend on DSL and generation layers, but it must not absorb CLI parsing, onboarding, or distribution concerns.
- `cli` is the application layer for command parsing, diagnostics, repository onboarding, plugin resolution, installation, and JetBrains IDE helper workflows. Lower layers must not depend on `cli`.
- `kotest` is test support only and should not become a production dependency surface.

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
- Default to the narrowest visibility that keeps the API honest. Widen visibility only when a real caller or extension point requires it.
- Use `sealed interface`, `data object`, `value class`, exhaustive `when`, and null-safety where they make state and invariants clearer.
- Use extension functions when they improve discoverability for a well-scoped domain operation and avoid creating utility dumping grounds.
- Use infix functions only for DSL-facing APIs when readability is clearly better than the non-infix equivalent.
- Use `object` and `companion object` only when singleton semantics, namespaced factories, or constants are genuinely clearer than top-level declarations or regular types.
- Avoid Java-style static utility patterns, unnecessary mutable state, and scope-function chains that hide control flow.

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
- If a structural exception is genuinely clearer than splitting the code, encode the exception in `build-logic/src/main/kotlin/me/liam/microsmith/build/quality/RepositoryQualityPolicy.kt` with a narrow path-specific rationale and call it out in the PR description.
- Broader architecture and layering decisions remain review-gated. If a rule cannot be automated without becoming brittle, explain the boundary explicitly in review.
- Reviewers should check:
  - file ownership and package placement are obvious
  - one-top-level-production-type-per-file remains the default
  - orchestration and side effects are separated from pure logic
  - Kotlin features improve clarity rather than novelty
  - `verifyRepositoryStandards`, `detekt`, `ktlintCheck`, and relevant tests remain green

## Build, test, and quality gates

Build the repository:

```bash
./gradlew clean build
```

Run tests:

```bash
./gradlew kotest
```

Run static analysis:

```bash
./gradlew detekt ktlintCheck
```

Run repository structural guardrails directly:

```bash
./gradlew verifyRepositoryStandards
```

Auto-format Kotlin sources:

```bash
./gradlew ktlintFormat
```

Useful notes:

- the Gradle build is configured for Java 24
- `./gradlew build` now includes the root `check` lifecycle and therefore runs `verifyRepositoryStandards`
- if `ktlintCheck` fails, run `./gradlew ktlintFormat` and rerun checks
- if `detekt` fails, inspect the generated report under `build/reports/detekt/`
- if `verifyRepositoryStandards` fails:
  - split extra production types into their own files or make tightly coupled helpers private
  - split large production files by responsibility before raising any threshold
  - add or correct explicit package declarations and keep them aligned with `src/main/kotlin` paths
  - rename single-type files so the file name matches the owning declaration
  - only add a path-specific exception in `build-logic/src/main/kotlin/me/liam/microsmith/build/quality/RepositoryQualityPolicy.kt` when the split would genuinely reduce clarity, and explain that exception in the PR

## DSL usage

### Core DSL

```kotlin
val model = microsmith {
    // schemas { ... }
}

val extension = model.get<YourExtensionType>()
val allExtensions = model.extensions()
val typedExtensions = model.extensions().filterIsInstance<YourExtensionType>()
```

### Protobuf schema DSL

```kotlin
microsmith {
    schemas {
        protobuf {
            message("UserCreated") {
                int32("id") { index(1) }
                string("email") { index(2) }
            }

            enum("UserStatus") {
                +"ACTIVE"
                +"DISABLED"
            }
        }
    }
}
```

### Script defaults

Inside `.microsmith.kts` scripts:

- default imports include `microsmith {}`, `schemas {}`, and `protobuf {}`
- a script can return a `MicrosmithModel`
- a script can also call `emit(model)` or `generate(model)`

## Standalone CLI for non-Gradle repositories

The CLI is the recommended entrypoint for Go, .NET, Node, and other repositories that do not use Gradle.
The official installer scripts are self-contained and provision a Java 24 runtime automatically when the machine does not already provide one.

Manual channels remain available, but they require Java 24 or newer.

### Recommended bootstrap flow

Install Microsmith, then initialize the repository and run generation:

```bash
microsmith init
microsmith run build.microsmith.kts --out ./generated
```

`microsmith init` is deterministic and non-destructive by default:

- it detects Node, Go, and .NET repositories from `package.json`, `go.mod`, `*.csproj`, and `*.sln`, and otherwise falls back to a generic bootstrap
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

After the canonical first run succeeds, you can switch to a repository-native output layout if you prefer:

- Node: `microsmith run build.microsmith.kts --out ./generated`
- Go: `microsmith run build.microsmith.kts --out ./internal/gen`
- .NET: `microsmith run build.microsmith.kts --out ./Generated`

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
microsmith run schema.microsmith.kts --out ./generated
```

Pass script context values when needed:

```bash
microsmith run schema.microsmith.kts --out ./generated --var env=prod --flag emit
```

Run in a separate JVM for stronger isolation:

```bash
microsmith run schema.microsmith.kts --out ./generated --isolation process
```

## Installation and verification

### macOS and Linux

```bash
VERSION=<microsmith-version>
curl -fsSL -o microsmith-install.sh "https://github.com/LMLiam/microsmith/releases/download/v${VERSION}/microsmith-install.sh"
sh microsmith-install.sh --version "${VERSION}"
```

### Windows (PowerShell)

```powershell
$Version = "<microsmith-version>"
Invoke-WebRequest -Uri "https://github.com/LMLiam/microsmith/releases/download/v$Version/microsmith-install.ps1" -OutFile microsmith-install.ps1
powershell -ExecutionPolicy Bypass -NoProfile -File .\microsmith-install.ps1 -Version $Version
```

### Verify immediately after installation

Use the installed shim path before opening a new shell:

```bash
"$HOME/.microsmith/bin/microsmith" --version
mkdir -p ./microsmith-smoke
"$HOME/.microsmith/bin/microsmith" init --repo-root ./microsmith-smoke
"$HOME/.microsmith/bin/microsmith" run ./microsmith-smoke/build.microsmith.kts --out ./microsmith-smoke/generated
```

```powershell
& (Join-Path $HOME ".microsmith\bin\microsmith.cmd") --version
New-Item -ItemType Directory -Path .\microsmith-smoke -Force | Out-Null
& (Join-Path $HOME ".microsmith\bin\microsmith.cmd") init --repo-root .\microsmith-smoke
& (Join-Path $HOME ".microsmith\bin\microsmith.cmd") run .\microsmith-smoke\build.microsmith.kts --out .\microsmith-smoke\generated
```

After opening a new shell, the bare `microsmith` command is available on `PATH`.

### Integrity verification

Release assets include SHA-256 sidecar files for:

- `microsmith-cli-<version>-all.jar`
- `microsmith-cli-<version>-dist.zip`
- `microsmith-cli-<version>-dist.tar.gz`
- `microsmith-install.sh`
- `microsmith-install.ps1`

Manual verification examples:

```bash
shasum -a 256 microsmith-install.sh
cat microsmith-install.sh.sha256
```

```bash
shasum -a 256 microsmith-cli-<version>-dist.tar.gz
cat microsmith-cli-<version>-dist.tar.gz.sha256
```

```powershell
Get-FileHash .\microsmith-install.ps1 -Algorithm SHA256
Get-Content .\microsmith-install.ps1.sha256
```

```powershell
Get-FileHash .\microsmith-cli-<version>-dist.zip -Algorithm SHA256
Get-Content .\microsmith-cli-<version>-dist.zip.sha256
```

Installer diagnostics explicitly cover:

- missing required tools such as `curl`, `tar`, `unzip`, and `python3` when automatic runtime metadata resolution is used
- checksum mismatch for CLI or runtime archives
- unsupported operating systems or architectures
- runtime provisioning failures
- missing Java 24+ when runtime provisioning is disabled

### Manual channels

The canonical onboarding path is the installer, but manual channels remain supported:

- fat jar: `java -jar microsmith-cli-<version>-all.jar --help`
- unpacked Unix distribution: `./microsmith-cli-<version>/bin/microsmith --help`
- unpacked Windows distribution: `.\microsmith-cli-<version>\bin\microsmith.bat --help`

Manual channels require Java 24 or newer.

## Command reference

Current CLI usage:

```text
Microsmith CLI

Usage:
  microsmith init [--repo-root <path>] [--force] [--skip-ide-helper]
                 [--diagnostics <text|json>] [--verbose]
  microsmith run <script.microsmith.kts> --out <output-dir> [--var <name=value>]... [--flag <name>]...
                 [--plugin <group:artifact:version>]... [--plugin-jar <path>]...
                 [--offline] [--repository <uri>] [--isolation <classloader|process>]
                 [--diagnostics <text|json>] [--verbose] [--event-log <path>]
  microsmith ide refresh [--repo-root <path>] [--diagnostics <text|json>] [--verbose]
  microsmith ide doctor [--repo-root <path>] [--diagnostics <text|json>] [--verbose]
  microsmith doctor [--diagnostics <text|json>] [--verbose]
  microsmith --version
  microsmith --help
```

### Canonical happy paths

Local:

```bash
microsmith init
microsmith run build.microsmith.kts --out ./generated
```

CI:

```bash
microsmith init --diagnostics json --verbose
microsmith run build.microsmith.kts --out ./generated --diagnostics json --verbose
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
- detects Node, Go, .NET, or generic repository shape and emits a matching starter script
- refreshes `.microsmith/ide` by default
- preserves existing regular bootstrap files unless `--force` is provided
- supports `--repo-root`, `--force`, `--skip-ide-helper`, `--diagnostics`, and `--verbose`

`microsmith run`

- executes a `.microsmith.kts` script and writes generated files under `--out`
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

## JetBrains IDE helper

Use the helper project when your repository is not Gradle-based and you want stronger `.microsmith.kts` type resolution in JetBrains IDEs such as IntelliJ IDEA, GoLand, and Rider.

`microsmith init` already refreshes the helper by default. Use `microsmith ide refresh` when you skipped helper generation during init or need to repair or resynchronize the helper later.

Generate or refresh the helper:

```bash
microsmith ide refresh
```

Validate helper health:

```bash
microsmith ide doctor --diagnostics json --verbose
```

Optional flags:

- `--repo-root <path>`
- `--diagnostics <text|json>`
- `--verbose`

Generated files:

- `.microsmith/ide/settings.gradle.kts`
- `.microsmith/ide/build.gradle.kts`
- `.microsmith/ide/README.md`

JetBrains workflow:

1. Run `microsmith init` from the repository root, or `microsmith ide refresh` if helper generation was skipped earlier.
2. Link or import `.microsmith/ide/build.gradle.kts` as a Gradle project in the IDE.
3. Refresh Gradle indexing.
4. Rerun `microsmith ide refresh` after upgrading the Microsmith CLI or changing plugin dependencies.

Important constraints:

- runtime generation does not depend on the IDE helper
- the helper build uses local file dependencies only
- repeated refreshes only rewrite files when content changes

## Plugin resolution and security model

### Plugin inputs

Use remote plugin coordinates:

```bash
microsmith run schema.microsmith.kts --out ./generated --plugin com.acme:microsmith-emitter-ts:1.4.2
```

Use local plugin jars:

```bash
microsmith run schema.microsmith.kts --out ./generated --plugin-jar ./plugins/emitter.jar
```

Run offline after cache warmup and lock generation:

```bash
microsmith run schema.microsmith.kts --out ./generated --offline
```

### Security defaults

Microsmith enforces the following defaults:

- script-time dependency directives such as `@file:DependsOn` and `@file:Repository` are blocked by default
- repository access is constrained by an allowlist policy
- the built-in allowed remote repository is Maven Central: `https://repo1.maven.org/maven2`
- additional allowed repositories can be configured with `MICROSMITH_REPOSITORY_ALLOWLIST`
- `file://` repositories are blocked by default and can be explicitly enabled with `MICROSMITH_ALLOW_FILE_REPOSITORIES=true`
- generated output writes are constrained to the configured `--out` root and reject traversal or symlink escapes
- the default execution mode is isolated per run; `--isolation process` moves execution into a separate JVM
- official CLI distributions include a pinned bundled plugin profile in `bundled-plugins.lock`

### Credentials and repository authentication

Credential precedence is deterministic:

1. `MICROSMITH_REPOSITORY_CREDENTIALS_FILE` with entries in the form `<repository-uri>|<username>|<password>`
2. GitHub Packages credentials for `https://maven.pkg.github.com` via `MICROSMITH_GITHUB_PACKAGES_USER` and `MICROSMITH_GITHUB_PACKAGES_TOKEN`, with fallback to `GITHUB_ACTOR` and `GITHUB_TOKEN`
3. global repository credentials via `MICROSMITH_REPOSITORY_USERNAME` and `MICROSMITH_REPOSITORY_PASSWORD`

Example using GitHub Packages:

```bash
export MICROSMITH_REPOSITORY_ALLOWLIST="https://maven.pkg.github.com/acme/microsmith"
export MICROSMITH_REPOSITORY_USERNAME="octocat"
export MICROSMITH_REPOSITORY_PASSWORD="$GITHUB_TOKEN"
microsmith run schema.microsmith.kts --out ./generated \
  --repository https://maven.pkg.github.com/acme/microsmith \
  --plugin com.acme:private-emitter:1.2.3
```

Example credentials file:

```text
# ~/.microsmith/repository-credentials.txt
https://maven.pkg.github.com/acme/microsmith|octocat|ghp_xxx
https://packages.acme.internal/maven|svc-microsmith|token-123
```

Use that file:

```bash
export MICROSMITH_REPOSITORY_CREDENTIALS_FILE="$HOME/.microsmith/repository-credentials.txt"
microsmith run schema.microsmith.kts --out ./generated --plugin com.acme:private-emitter:1.2.3
```

### Environment variables

| Variable                                 | Purpose                                                               |
|------------------------------------------|-----------------------------------------------------------------------|
| `MICROSMITH_REPOSITORY_ALLOWLIST`        | Comma-separated additional allowed repository base URIs.              |
| `MICROSMITH_ALLOW_FILE_REPOSITORIES`     | Set to `true` to allow `file://` repositories for plugin coordinates. |
| `MICROSMITH_REPOSITORY_CREDENTIALS_FILE` | Path to a repository credentials file.                                |
| `MICROSMITH_REPOSITORY_USERNAME`         | Default username for authenticated repository access.                 |
| `MICROSMITH_REPOSITORY_PASSWORD`         | Default password or token for authenticated repository access.        |
| `MICROSMITH_GITHUB_PACKAGES_USER`        | Username for GitHub Packages repository access.                       |
| `MICROSMITH_GITHUB_PACKAGES_TOKEN`       | Token for GitHub Packages repository access.                          |
| `MICROSMITH_PLUGIN_ALLOWLIST_FILE`       | Path to checksum allowlist file entries in the form `<code>&lt;kind&gt;&#124;&lt;key&gt;&#124;&lt;sha256&gt;</code>`. |
| `MICROSMITH_SCRIPT_CACHE_DIR`            | Override the script compilation cache directory.                      |
| `MICROSMITH_PLUGIN_CACHE_DIR`            | Override the plugin resolution cache directory.                       |

## CI examples

Set repository or environment variables:

- `MICROSMITH_VERSION`
- `MICROSMITH_INSTALLER_SH_URL`, for example `https://github.com/LMLiam/microsmith/releases/download/v<version>/microsmith-install.sh`
- `MICROSMITH_INSTALLER_PS1_URL`, for example `https://github.com/LMLiam/microsmith/releases/download/v<version>/microsmith-install.ps1`

### Node repository

```yaml
name: Microsmith Generate
on:
  pull_request:
  push:
    branches: [main]
jobs:
  generate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v5
      - name: Install Microsmith CLI
        run: |
          curl -fsSL -o microsmith-install.sh "$MICROSMITH_INSTALLER_SH_URL"
          sh microsmith-install.sh --version "$MICROSMITH_VERSION"
          echo "$HOME/.microsmith/bin" >> "$GITHUB_PATH"
      - name: Bootstrap Microsmith
        run: microsmith init
      - name: Run Microsmith
        run: microsmith run build.microsmith.kts --out generated
```

### Go repository

```yaml
name: Microsmith Generate
on:
  pull_request:
jobs:
  generate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v5
      - name: Install Microsmith CLI
        run: |
          curl -fsSL -o microsmith-install.sh "$MICROSMITH_INSTALLER_SH_URL"
          sh microsmith-install.sh --version "$MICROSMITH_VERSION"
          echo "$HOME/.microsmith/bin" >> "$GITHUB_PATH"
      - name: Bootstrap Microsmith
        run: microsmith init
      - name: Generate protobuf
        run: microsmith run build.microsmith.kts --out internal/gen
```

### .NET repository

```yaml
name: Microsmith Generate
on:
  pull_request:
jobs:
  generate:
    runs-on: windows-latest
    steps:
      - uses: actions/checkout@v5
      - name: Install Microsmith CLI
        shell: pwsh
        run: |
          Invoke-WebRequest -Uri $env:MICROSMITH_INSTALLER_PS1_URL -OutFile microsmith-install.ps1
          .\microsmith-install.ps1 -Version $env:MICROSMITH_VERSION
          Add-Content -Path $env:GITHUB_PATH -Value (Join-Path $HOME ".microsmith\bin")
      - name: Bootstrap Microsmith
        shell: pwsh
        run: microsmith init
      - name: Run Microsmith
        shell: pwsh
        run: microsmith run build.microsmith.kts --out .\Generated
```

## Example fixtures

The repository includes non-Gradle fixture repositories under `examples/non-gradle/`.
Each fixture contains a repo marker used by `microsmith init`, a legacy `schema.microsmith.kts` manual example, and a GitHub Actions example that exercises the init-first path.

| Fixture | Directory                    | Local command from fixture root                                                   | CI workflow                                                   |
|---------|------------------------------|-----------------------------------------------------------------------------------|---------------------------------------------------------------|
| Node    | `examples/non-gradle/node`   | `microsmith init` then `microsmith run build.microsmith.kts --out ./generated`    | `examples/non-gradle/node/.github/workflows/microsmith.yml`   |
| Go      | `examples/non-gradle/go`     | `microsmith init` then `microsmith run build.microsmith.kts --out ./internal/gen` | `examples/non-gradle/go/.github/workflows/microsmith.yml`     |
| .NET    | `examples/non-gradle/dotnet` | `microsmith init` then `microsmith run build.microsmith.kts --out .\Generated`    | `examples/non-gradle/dotnet/.github/workflows/microsmith.yml` |

## Troubleshooting

### Java runtime issues

Symptoms:

- `java` not found
- unsupported class version or runtime mismatch

What to do:

- use the canonical installer path so Java 24 is provisioned automatically when needed
- if you are using a manual channel, install Java 24+ and set `JAVA_HOME`
- rerun `microsmith --version` and `microsmith --help`

### Installer failures

Symptoms:

- installer exits non-zero
- checksum mismatch during installation
- runtime provisioning fails

What to do:

- verify the relevant `*.sha256` file and rerun the installer
- use explicit archive and checksum flags when diagnosing a specific asset
- inspect installer output for missing prerequisites such as `curl`, `tar`, `unzip`, or `python3`
- do not combine `--force-runtime-provision` and `--skip-runtime-provision`

### Script and init failures

Symptoms:

- `microsmith run` exits with compile or evaluation errors
- `microsmith init` exits non-zero
- expected bootstrap files are missing

What to do:

- confirm the script file ends with `.microsmith.kts`
- rerun with `--diagnostics json --verbose`
- ensure `--repo-root` points to an existing directory
- run `microsmith doctor --diagnostics json --verbose` to detect incomplete bootstrap state
- resolve filesystem conflicts such as a directory already existing at `build.microsmith.kts`
- rerun `microsmith init --force` if you want the managed bootstrap scripts to replace existing regular files
- if helper generation was skipped earlier, run `microsmith ide refresh` before importing `.microsmith/ide` into JetBrains IDEs

### Resolver, credentials, and offline failures

Symptoms:

- plugin coordinate cannot be resolved
- repository URI is rejected
- authentication fails
- checksum or lockfile validation fails
- `--offline` cannot resolve plugins

What to do:

- validate coordinate syntax: `group:artifact:version`
- confirm the repository is in the allowlist
- configure credentials using the precedence described above
- run once without `--offline` to warm the cache and write lock metadata
- ensure the complete locked dependency graph exists in the plugin cache
- if strict checksum allowlisting is enabled, include transitive entries such as `remote-artifact|<cache-relative-path>|<sha256>`

### Built-in provider or IDE helper failures

Symptoms:

- built-in generators or emitters are reported missing
- `.microsmith.kts` files still show unresolved Microsmith symbols in JetBrains IDEs
- helper project appears stale after a CLI upgrade

What to do:

- use the official CLI distribution or installer
- run `microsmith doctor --diagnostics json --verbose`
- run `microsmith ide doctor --diagnostics json --verbose`
- rerun `microsmith ide refresh`
- in JetBrains IDEs, re-import or refresh `.microsmith/ide/build.gradle.kts`
- if you are validating release assets in this repository, run `./gradlew :cli:verifyShadowJarServices` and `./gradlew :cli:verifyDistLayout`

## Migration from Gradle

Use the CLI when you want generation in repositories that do not carry a Gradle wrapper.
The DSL surface and plugin architecture stay the same; the execution model changes.

### Command mapping

| Previous pattern                 | CLI replacement                                          |
|----------------------------------|----------------------------------------------------------|
| Gradle task invoking generation  | `microsmith run schema.microsmith.kts --out ./generated` |
| Gradle-managed plugin dependency | `--plugin group:artifact:version`                        |
| local classpath jar wiring       | `--plugin-jar ./path/to/plugin.jar`                      |
| Gradle offline mode              | `--offline`                                              |

### Recommended migration sequence

1. Move the DSL into `*.microsmith.kts` files.
2. Replace Gradle-based generation commands in CI with `microsmith run`.
3. Pin plugin coordinates and validate lock or checksum behavior for reproducibility.
4. Enable the relevant security controls for your environment.
5. Remove obsolete Gradle generation tasks after parity is proven.

## Distribution and release artifacts

### Release asset contents

`./gradlew :cli:releaseArtifacts` produces:

- `cli/build/libs/microsmith-cli-<version>-all.jar`
- `cli/build/distributions/microsmith-cli-<version>-dist.zip`
- `cli/build/distributions/microsmith-cli-<version>-dist.tar.gz`
- `cli/build/release-assets/microsmith-install.sh`
- `cli/build/release-assets/microsmith-install.ps1`
- `cli/build/release-assets/*.sha256`
- `cli/build/generated/microsmith/bundled-plugins.lock`

The bundled plugin catalog is packaged into the official artifacts and pinned to the CLI version.
Published packages are available from:

```text
https://maven.pkg.github.com/lmliam/microsmith
```

### Useful build tasks

- `:cli:generateBundledPluginCatalog`
- `:cli:shadowJar`
- `:cli:prepareDist`
- `:cli:cliDistZip`
- `:cli:cliDistTar`
- `:cli:verifyDistLayout`
- `:cli:distArtifacts` for internal staging into `cli/build/release-assets/`
- `:cli:generateReleaseChecksums`
- `:cli:releaseArtifacts`

### Current packaging model

Microsmith currently ships with:

- installer scripts that provision Java 24 automatically when needed
- manual fat jar and unpacked distribution channels for teams that want explicit runtime management
- checksum sidecars for release verification
- no requirement for Gradle or Maven in consuming repositories
