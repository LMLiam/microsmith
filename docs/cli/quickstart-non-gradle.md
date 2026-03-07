# Gradle-less Quickstart (Node, Go, .NET)

This guide shows how to run Microsmith CLI from repositories that do not use Gradle.

## Prerequisites

- Install Microsmith using the canonical OS installer path from `install.md`.
- If using manual channels instead of installer scripts, Java 24+ is required.

## Recommended bootstrap path

Install check:

If you are verifying immediately after installation, use the direct shim path from `install.md`.
After opening a new shell, the bare `microsmith` command is available on `PATH`.

```bash
microsmith --version
```

Initialize repository defaults and IDE helper metadata:

```bash
microsmith init
```

Then run generation from the default bootstrap script:

```bash
microsmith run build.microsmith.kts --out ./generated
```

For CI:

```bash
microsmith init --non-interactive --yes --diagnostics json --verbose
```

## Manual script path

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

## JetBrains IDE helper

Generate helper project metadata for JetBrains IDE indexing:

```bash
microsmith ide refresh
```

Validate helper state and detect stale classpath metadata:

```bash
microsmith ide doctor --diagnostics json --verbose
```

If your repository root is not the current working directory:

```bash
microsmith ide refresh --repo-root ./path/to/repo
```

See `jetbrains-ide-helper.md` for full setup and troubleshooting details.

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

Set repository/environment variables:

- `MICROSMITH_VERSION` (for example, `1.2.3`)
- `MICROSMITH_INSTALLER_SH_URL`: `https://github.com/LMLiam/microsmith/releases/download/v<version>/microsmith-install.sh`
- `MICROSMITH_INSTALLER_PS1_URL`: `https://github.com/LMLiam/microsmith/releases/download/v<version>/microsmith-install.ps1`

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
      - name: Run Microsmith
        run: microsmith run schema.microsmith.kts --out generated/proto
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
      - name: Generate protobuf
        run: microsmith run schema.microsmith.kts --out internal/gen/proto
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
          Add-Content -Path $env:GITHUB_PATH -Value (Join-Path $HOME ".microsmith\\bin")
      - name: Run Microsmith
        shell: pwsh
        run: microsmith run schema.microsmith.kts --out .\Generated\Proto
```

See `troubleshooting.md` for resolver, offline mode, and diagnostics details.
