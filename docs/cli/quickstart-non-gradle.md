# Gradle-less Quickstart (Node, Go, .NET)

This guide shows how to run Microsmith CLI from repositories that do not use Gradle.

## Prerequisites

- Java 24 or newer on `PATH` (or set `JAVA_HOME`).
- A Microsmith CLI distribution artifact:
  - `microsmith-cli-<version>-all.jar`, or
  - `microsmith-cli-<version>-dist.zip` / `microsmith-cli-<version>-dist.tar.gz`.

## Minimal script

Create `schema.microsmith.kts`:

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

If using the unpacked distribution:

```bash
./tools/microsmith-cli-*/bin/microsmith run schema.microsmith.kts --out ./generated
```

Windows:

```powershell
.\tools\microsmith-cli-*\bin\microsmith.bat run schema.microsmith.kts --out .\generated
```

## Plugin extension workflow

Bundled plugin workflow (no network required):

```bash
microsmith run schema.microsmith.kts --out ./generated --offline
```

The official distribution includes a pinned bundled plugin profile in `bundled-plugins.lock`.
It contains the built-in providers required for schema/protobuf generation and is versioned with the CLI release.

Remote plugin coordinate:

```bash
microsmith run schema.microsmith.kts --out ./generated --plugin com.acme:microsmith-emitter-ts:1.4.2
```

Local plugin jar:

```bash
microsmith run schema.microsmith.kts --out ./generated --plugin-jar ./plugins/emitter.jar
```

Offline mode:

```bash
microsmith run schema.microsmith.kts --out ./generated --offline
```

Notes:
- Bundled plugin workflows do not require repository access.
- Run once without `--offline` first to generate lockfile metadata and warm the plugin cache.
- Offline remote plugin resolution requires lockfile v2 and a complete cached dependency graph.

Authenticated private repository (global credentials):

```bash
export MICROSMITH_REPOSITORY_ALLOWLIST="https://maven.pkg.github.com/acme/microsmith"
export MICROSMITH_REPOSITORY_USERNAME="octocat"
export MICROSMITH_REPOSITORY_PASSWORD="$GITHUB_TOKEN"
microsmith run schema.microsmith.kts --out ./generated \
  --repository https://maven.pkg.github.com/acme/microsmith \
  --plugin com.acme:private-emitter:1.2.3
```

Per-repository credentials file:

```text
# ~/.microsmith/repository-credentials.txt
https://maven.pkg.github.com/acme/microsmith|octocat|ghp_xxx
https://packages.acme.internal/maven|svc-microsmith|token-123
```

```bash
export MICROSMITH_REPOSITORY_CREDENTIALS_FILE="$HOME/.microsmith/repository-credentials.txt"
microsmith run schema.microsmith.kts --out ./generated --plugin com.acme:private-emitter:1.2.3
```

Security defaults are enabled by default:

- script dependency directives are blocked
- plugin repositories are policy-constrained
- output writes are restricted to `--out`
- plugin checksums are validated when lock/allowlist metadata exists

## GitHub Actions snippets

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
      - uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: 24
      - name: Download Microsmith CLI
        run: |
          mkdir -p tools
          curl -sSL -o tools/microsmith.zip "$MICROSMITH_DIST_URL"
          unzip -q tools/microsmith.zip -d tools
      - name: Run Microsmith
        run: ./tools/microsmith-cli-*/bin/microsmith run schema.microsmith.kts --out generated/proto
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
      - uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: 24
      - name: Fetch CLI jar
        run: curl -sSL -o microsmith-cli.jar "$MICROSMITH_CLI_JAR_URL"
      - name: Generate protobuf
        run: java -jar microsmith-cli.jar run schema.microsmith.kts --out internal/gen/proto
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
      - uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: 24
      - name: Download CLI distribution
        shell: pwsh
        run: |
          New-Item -ItemType Directory -Path tools -Force | Out-Null
          Invoke-WebRequest -Uri $env:MICROSMITH_DIST_URL -OutFile tools\microsmith.zip
          Expand-Archive -Path tools\microsmith.zip -DestinationPath tools -Force
      - name: Run Microsmith
        shell: pwsh
        run: .\tools\microsmith-cli-*\bin\microsmith.bat run schema.microsmith.kts --out .\Generated\Proto
```

See `troubleshooting.md` for resolver, offline mode, and diagnostics details.
